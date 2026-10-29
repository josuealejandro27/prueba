package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import com.proyecto.servicios.service.PagoFinancieroService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class PagoFinancieroServiceImpl implements PagoFinancieroService {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransaccionPersistenceHelper persistenceHelper;
    private final GestoPagoCryptoService cryptoService;
    private final GestoPagoTxClient txClient;
    private final TransaccionFinancieraRepository transaccionRepository;

    public PagoFinancieroServiceImpl(TransaccionPersistenceHelper persistenceHelper,
                                     GestoPagoCryptoService cryptoService,
                                     GestoPagoTxClient txClient,
                                     TransaccionFinancieraRepository transaccionRepository) {
        this.persistenceHelper = persistenceHelper;
        this.cryptoService = cryptoService;
        this.txClient = txClient;
        this.transaccionRepository = transaccionRepository;
    }

    @Override
    public PagoResponse procesarPago(PagoRequest request) {
        log.info("Iniciando procesamiento de pago: cuenta={}, monto={}, idServicio={}, idempotencyKey={}",
                request.getNumeroCuenta(), request.getMonto(), request.getIdServicio(), request.getIdempotencyKey());

        // 1. Verificación de idempotencia
        Optional<PagoResponse> previa = persistenceHelper.verificarIdempotencia(request.getIdempotencyKey());
        if (previa.isPresent() && previa.get().getEstado() == EstadoTransaccion.EXITOSA) {
            log.info("Idempotencia: retornando transacción exitosa previa con clave {}", request.getIdempotencyKey());
            return previa.get();
        }

        // 2. Fase 1: Bloqueo pesimista, validación de cuenta/PIN/saldo y reserva local (transacción corta)
        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(request);
        log.info("Fase 1 completada: saldo reservado para tx folio={}", tx.getFolio());

        // Si ya estaba completada (reintento previo)
        if (tx.getEstado() == EstadoTransaccion.EXITOSA) {
            return persistenceHelper.construirPagoResponse(tx);
        }

        // 3. Fase 2: Construcción de payload, firma AES y consumo de Gestopago (fuera de transacción BD)
        GestoPagoTxResponse responseGestopago = null;
        try {
            String horaLocal = LocalDateTime.now().format(FORMATO_HORA);
            String upc = request.getUpc() != null && !request.getUpc().isBlank()
                    ? request.getUpc().trim() : String.valueOf(request.getIdProducto());

            GestoPagoTxRequest txRequest = GestoPagoTxRequest.builder()
                    .idProveedor(request.getIdDistribuidor())
                    .idService(request.getIdServicio())
                    .telefono(request.getReferencia())
                    .horaLocal(horaLocal)
                    .montoPago(request.getMonto())
                    .upc(upc)
                    .unidad("1")
                    .build();

            // Firmar con AES
            String firma = cryptoService.firmar(txRequest);
            txRequest.setFirma(firma);

            log.info("Enviando transacción a Gestopago sendTx.do: idProveedor={}, idService={}, ref={}",
                    txRequest.getIdProveedor(), txRequest.getIdService(), txRequest.getTelefono());

            responseGestopago = txClient.enviarTransaccion(txRequest);
            log.info("Respuesta recibida de Gestopago: codigo={}, folio={}, mensaje={}",
                    responseGestopago != null ? responseGestopago.getCodigo() : "null",
                    responseGestopago != null ? responseGestopago.getFolio() : "null",
                    responseGestopago != null ? responseGestopago.getMensaje() : "null");

        } catch (Exception e) {
            log.error("Fallo de comunicación al invocar sendTx.do para tx folio {}: {}", tx.getFolio(), e.getMessage());

            if (esTimeout(e)) {
                TransaccionFinanciera txTimeout = persistenceHelper.marcarTimeout(tx.getId(), e.getMessage());
                return persistenceHelper.construirPagoResponse(txTimeout);
            } else {
                TransaccionFinanciera txRevertida = persistenceHelper.revertirPorFallaComunicacion(tx.getId(), e.getMessage());
                return persistenceHelper.construirPagoResponse(txRevertida);
            }
        }

        // 4. Fase 3: Liquidación local (transacción corta)
        if (responseGestopago != null && responseGestopago.esExitosa()) {
            log.info("Transacción aprobada por Gestopago (codigo={}). Consolidando débito para folio {}",
                    responseGestopago.getCodigo(), tx.getFolio());
            TransaccionFinanciera txExitosa = persistenceHelper.liquidarExito(tx.getId(), responseGestopago);
            return persistenceHelper.construirPagoResponse(txExitosa);
        } else {
            log.warn("Transacción rechazada por Gestopago (codigo={}). Reversando saldo para folio {}",
                    responseGestopago != null ? responseGestopago.getCodigo() : "null", tx.getFolio());
            TransaccionFinanciera txRechazada = persistenceHelper.liquidarRechazo(tx.getId(), responseGestopago);
            return persistenceHelper.construirPagoResponse(txRechazada);
        }
    }

    @Override
    public PagoResponse consultarTransaccion(ConsultaTransaccionRequest request) {
        String folioOClave = request.getFolio().trim();
        TransaccionFinanciera tx = transaccionRepository.findByFolio(folioOClave)
                .or(() -> transaccionRepository.findByIdempotencyKey(folioOClave))
                .orElseThrow(() -> new CuentaNoEncontradaException("Transacción no encontrada con folio/clave: " + folioOClave));

        return persistenceHelper.construirPagoResponse(tx);
    }

    private boolean esTimeout(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException
                    || (cause instanceof FeignException && ((FeignException) cause).status() == 504)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

