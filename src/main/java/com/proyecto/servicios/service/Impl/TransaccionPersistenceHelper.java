package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.TipoOperacion;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class TransaccionPersistenceHelper {

    private final CuentaBancariaRepository cuentaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TransaccionPersistenceHelper(CuentaBancariaRepository cuentaRepository,
                                        TransaccionFinancieraRepository transaccionRepository,
                                        UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<PagoResponse> verificarIdempotencia(String idempotencyKey) {
        return transaccionRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::construirPagoResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera iniciarYReservarSaldo(PagoRequest request) {
        // Idempotencia check
        if (transaccionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            TransaccionFinanciera existente = transaccionRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElseThrow();
            if (existente.getEstado() == EstadoTransaccion.EN_PROCESO) {
                throw new TransaccionIdempotenciaException(request.getIdempotencyKey());
            }
            return existente;
        }

        // Bloqueo pesimista de cuenta
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(request.getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(request.getNumeroCuenta()));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new CuentaInactivaException(cuenta.getNumeroCuenta(), cuenta.getEstatus());
        }

        if (cuenta.getSaldo().compareTo(request.getMonto()) < 0) {
            throw new SaldoInsuficienteException(cuenta.getNumeroCuenta(), cuenta.getSaldo(), request.getMonto());
        }

        // Validación de PIN
        validarPin(cuenta, request.getPin());

        // Reserva de saldo
        cuenta.setSaldo(cuenta.getSaldo().subtract(request.getMonto()));
        cuentaRepository.save(cuenta);

        // Registro de la transacción en proceso
        TransaccionFinanciera tx = TransaccionFinanciera.builder()
                .folio(UUID.randomUUID().toString())
                .idempotencyKey(request.getIdempotencyKey())
                .cuenta(cuenta)
                .monto(request.getMonto())
                .tipoOperacion(TipoOperacion.PAGO_SERVICIO)
                .estado(EstadoTransaccion.EN_PROCESO)
                .idDistribuidor(request.getIdDistribuidor())
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .referencia(request.getReferencia())
                .build();

        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera liquidarExito(Long txId, GestoPagoTxResponse response) {
        TransaccionFinanciera tx = transaccionRepository.findById(txId).orElseThrow();
        tx.setEstado(EstadoTransaccion.EXITOSA);
        tx.setCodigoRespuestaProveedor(response != null && response.getCodigo() != null ? response.getCodigo() : "0");
        tx.setMensajeProveedor(response != null && response.getMensaje() != null ? response.getMensaje() : "TRANSACCION EXITOSA");
        tx.setFolioProveedor(response != null ? response.getFolio() : null);
        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera liquidarRechazo(Long txId, GestoPagoTxResponse response) {
        TransaccionFinanciera tx = transaccionRepository.findById(txId).orElseThrow();

        // Reversión de saldo a la cuenta
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(tx.getCuenta().getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getNumeroCuenta()));
        cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
        cuentaRepository.save(cuenta);

        tx.setEstado(EstadoTransaccion.RECHAZADA);
        tx.setCodigoRespuestaProveedor(response != null && response.getCodigo() != null ? response.getCodigo() : "RECHAZADO");
        tx.setMensajeProveedor(response != null && response.getMensaje() != null ? response.getMensaje() : "Transacción rechazada por el proveedor");
        tx.setFolioProveedor(response != null ? response.getFolio() : null);
        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera marcarTimeout(Long txId, String detalle) {
        TransaccionFinanciera tx = transaccionRepository.findById(txId).orElseThrow();
        tx.setEstado(EstadoTransaccion.TIMEOUT);
        tx.setCodigoRespuestaProveedor("TIMEOUT");
        tx.setMensajeProveedor("Timeout de comunicación con Gestopago: " + detalle);
        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera revertirPorFallaComunicacion(Long txId, String detalle) {
        TransaccionFinanciera tx = transaccionRepository.findById(txId).orElseThrow();

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(tx.getCuenta().getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getNumeroCuenta()));
        cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
        cuentaRepository.save(cuenta);

        tx.setEstado(EstadoTransaccion.RECHAZADA);
        tx.setCodigoRespuestaProveedor("ERROR_COMUNICACION");
        tx.setMensajeProveedor("Fallo de comunicación con Gestopago: " + detalle);
        return transaccionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public PagoResponse construirPagoResponse(TransaccionFinanciera tx) {
        String numCuenta = tx.getCuenta() != null ? tx.getCuenta().getNumeroCuenta() : null;
        BigDecimal saldoRestante = numCuenta != null
                ? cuentaRepository.findByNumeroCuenta(numCuenta)
                    .map(CuentaBancaria::getSaldo)
                    .orElse(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        return PagoResponse.builder()
                .folio(tx.getFolio())
                .idempotencyKey(tx.getIdempotencyKey())
                .numeroCuenta(numCuenta)
                .monto(tx.getMonto())
                .estado(tx.getEstado())
                .codigoRespuestaProveedor(tx.getCodigoRespuestaProveedor())
                .mensajeProveedor(tx.getMensajeProveedor())
                .folioProveedor(tx.getFolioProveedor())
                .saldoRestante(saldoRestante)
                .fechaTransaccion(tx.getFechaActualizacion() != null ? tx.getFechaActualizacion() : tx.getFechaCreacion())
                .build();
    }

    private void validarPin(CuentaBancaria cuenta, String pinIngresado) {
        if (cuenta.getPersonaFisica() != null && cuenta.getPersonaFisica().getId() != null) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByClienteId(cuenta.getPersonaFisica().getId());
            if (usuarioOpt.isPresent()) {
                if (!passwordEncoder.matches(pinIngresado, usuarioOpt.get().getPassword())) {
                    throw new PinInvalidoException();
                }
                return;
            }
        }
        if (pinIngresado == null || pinIngresado.isBlank()) {
            throw new PinInvalidoException();
        }
    }
}

