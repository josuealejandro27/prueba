package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.exception.CuentaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.TransaccionIdempotenciaException;
import com.proyecto.servicios.model.catalogo.ProductoDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.repositorys.mongo.CatalogoProductosCacheRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoFinancieroService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PagoFinancieroServiceImpl implements PagoFinancieroService {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PATRON_TELEFONO_10 = Pattern.compile("^\\d{10}$");
    private static final Pattern PATRON_NUMERICO = Pattern.compile("^\\d{4,40}$");
    private static final Pattern PATRON_REFERENCIA_ALFANUM = Pattern.compile("^[A-Za-z0-9\\-_]{4,100}$");

    private final TransaccionPersistenceHelper persistenceHelper;
    private final GestoPagoCryptoService cryptoService;
    private final GestoPagoTxClient txClient;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final CatalogoProductosCacheRepository cacheRepository;
    private final GestoPagoTokenService tokenService;
    private final Integer idDistribuidorConfigurado;

    private final Map<String, ReentrantLock> locksPorIdempotencia = new ConcurrentHashMap<>();

    public PagoFinancieroServiceImpl(TransaccionPersistenceHelper persistenceHelper,
                                     GestoPagoCryptoService cryptoService,
                                     GestoPagoTxClient txClient,
                                     TransaccionFinancieraRepository transaccionRepository) {
        this(persistenceHelper, cryptoService, txClient, transaccionRepository, null, null, 0);
    }

    @Autowired
    public PagoFinancieroServiceImpl(TransaccionPersistenceHelper persistenceHelper,
                                     GestoPagoCryptoService cryptoService,
                                     GestoPagoTxClient txClient,
                                     TransaccionFinancieraRepository transaccionRepository,
                                     CatalogoProductosCacheRepository cacheRepository,
                                     GestoPagoTokenService tokenService,
                                     @Value("${gestopago.auth.id-distribuidor:0}") Integer idDistribuidorConfigurado) {
        this.persistenceHelper = persistenceHelper;
        this.cryptoService = cryptoService;
        this.txClient = txClient;
        this.transaccionRepository = transaccionRepository;
        this.cacheRepository = cacheRepository;
        this.tokenService = tokenService;
        this.idDistribuidorConfigurado = idDistribuidorConfigurado;
    }

    @Override
    public PagoResponse procesarPago(PagoRequest request) {
        String idempotencyKey = request.getIdempotencyKey() != null ? request.getIdempotencyKey().trim() : "";
        request.setIdempotencyKey(idempotencyKey);

        log.info("Iniciando procesamiento de pago: cuenta={}, monto={}, idServicio={}, idProducto={}, idempotencyKey={}",
                request.getNumeroCuenta(), request.getMonto(), request.getIdServicio(),
                request.getIdProducto(), idempotencyKey);

        ReentrantLock lock = locksPorIdempotencia.computeIfAbsent(idempotencyKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.warn("Solicitud simultánea bloqueada para la misma idempotencyKey={}", idempotencyKey);
            throw new TransaccionIdempotenciaException(idempotencyKey);
        }

        try {
            // 1. Verificación integral de idempotencia (cubre TODOS los estados: EXITOSA, RECHAZADA, TIMEOUT, EN_PROCESO, REVERTIDA)
            Optional<TransaccionFinanciera> txExistenteOpt = persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(request);
            if (txExistenteOpt.isPresent()) {
                return resolverRespuestaIdempotente(txExistenteOpt.get(), request);
            }

            Optional<PagoResponse> previa = persistenceHelper.verificarIdempotencia(idempotencyKey);
            if (previa.isPresent()) {
                return resolverPagoResponsePrevia(previa.get(), request);
            }

            // 2. Validación financiera contra el catálogo almacenado en el servidor (Etapa 2)
            validarOperacionContraCatalogo(request);

            // 3. Fase 1: Bloqueo pesimista, validación de propiedad/cliente/cuenta/PIN/saldo y reserva local
            TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(request);
            log.info("Fase 1 completada para tx folio={} con estado={}", tx.getFolio(), tx.getEstado());

            // Si iniciarYReservarSaldo devolvió una transacción ya procesada (EXITOSA, RECHAZADA o TIMEOUT),
            // NUNCA volver a cobrar ni liquidar nuevamente.
            if (tx.getEstado() != EstadoTransaccion.EN_PROCESO) {
                log.info("Idempotencia en Fase 1: retornando transacción existente folio={} en estado={} sin reenviar al proveedor",
                        tx.getFolio(), tx.getEstado());
                return persistenceHelper.construirPagoResponse(tx);
            }

            // 4. Fase 2: Construcción de payload, firma AES y consumo de Gestopago (fuera de transacción BD)
            GestoPagoTxResponse responseGestopago;
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

                // Firmar con AES antes de marcar envío al proveedor (si falla localmente la criptografía, no se envió)
                String firma = cryptoService.firmar(txRequest);
                txRequest.setFirma(firma);

                // Registrar auditoría de que la petición sale hacia el proveedor externo
                persistenceHelper.marcarEnviandoAProveedor(tx.getId());

                log.info("Enviando transacción a Gestopago sendTx.do: idProveedor={}, idService={}, ref={}",
                        txRequest.getIdProveedor(), txRequest.getIdService(), txRequest.getTelefono());

                responseGestopago = txClient.enviarTransaccion(txRequest);
                log.info("Respuesta recibida de Gestopago: codigo={}, folio={}, mensaje={}",
                        responseGestopago != null ? responseGestopago.getCodigo() : "null",
                        responseGestopago != null ? responseGestopago.getFolio() : "null",
                        responseGestopago != null ? responseGestopago.getMensaje() : "null");

                if (responseGestopago != null && "06".equals(responseGestopago.getCodigo()) && tokenService != null) {
                    log.warn("Gestopago reportó código 06 (token expirado/inválido). Solicitando renovación de token.");
                    tokenService.renovarToken();
                }

            } catch (Exception e) {
                log.error("Fallo de comunicación al invocar sendTx.do para tx folio {}: {}", tx.getFolio(), e.getMessage());

                if (esFalloAutenticacionProveedor(e) && tokenService != null) {
                    tokenService.renovarToken();
                }

                if (esTimeout(e)) {
                    TransaccionFinanciera txTimeout = persistenceHelper.marcarTimeout(tx.getId(), e.getMessage());
                    return persistenceHelper.construirPagoResponse(txTimeout);
                } else {
                    TransaccionFinanciera txRevertida = persistenceHelper.revertirPorFallaComunicacion(tx.getId(), e.getMessage());
                    return persistenceHelper.construirPagoResponse(txRevertida);
                }
            }

            // 5. Fase 3: Liquidación local controlada (transacción corta con validación de estado previo)
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
        } finally {
            lock.unlock();
            locksPorIdempotencia.remove(idempotencyKey, lock);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagoResponse consultarTransaccion(ConsultaTransaccionRequest request) {
        String folioOClave = request.getFolio().trim();
        TransaccionFinanciera tx = transaccionRepository.findByFolio(folioOClave)
                .or(() -> transaccionRepository.findByIdempotencyKey(folioOClave))
                .orElseThrow(() -> new CuentaNoEncontradaException("Transacción no encontrada con folio/clave: " + folioOClave));

        // Validar que la transacción consultada pertenezca al usuario autenticado en el JWT (Anti-IDOR)
        persistenceHelper.validarPropiedadCuenta(tx.getCuenta());

        return persistenceHelper.construirPagoResponse(tx);
    }

    @Override
    public int recuperarTransaccionesPendientes(LocalDateTime umbral) {
        return persistenceHelper.recuperarTransaccionesPendientes(umbral);
    }

    @Override
    public PagoResponse conciliarTransaccionEnCustodia(Long txId,
                                                       boolean confirmadaExitosaEnProveedor,
                                                       String folioProveedor,
                                                       String motivoAuditoria) {
        TransaccionFinanciera tx = persistenceHelper.conciliarTransaccionEnCustodia(
                txId, confirmadaExitosaEnProveedor, folioProveedor, motivoAuditoria);
        return persistenceHelper.construirPagoResponse(tx);
    }

    @Scheduled(fixedDelayString = "${gestopago.tx.recuperacion.intervalo-ms:60000}")
    public void tareaProgramadaRecuperacionPendientes() {
        try {
            LocalDateTime umbral = LocalDateTime.now().minusSeconds(60);
            int recuperadas = recuperarTransaccionesPendientes(umbral);
            if (recuperadas > 0) {
                log.info("Tarea de recuperación procesó {} transacción(es) pendiente(s)", recuperadas);
            }
        } catch (Exception e) {
            log.error("Error ejecutando tarea programada de recuperación de transacciones: {}", e.getMessage());
        }
    }

    private PagoResponse resolverRespuestaIdempotente(TransaccionFinanciera existente, PagoRequest request) {
        EstadoTransaccion estado = existente.getEstado();
        if (estado == EstadoTransaccion.EN_PROCESO || estado == EstadoTransaccion.INICIADA) {
            throw new TransaccionIdempotenciaException(request.getIdempotencyKey());
        }
        if (estado == EstadoTransaccion.REVERTIDA) {
            throw new CuentaException("TRANSACCION_REVERTIDA",
                    "La clave de idempotencia pertenece a una operación revertida y no puede reutilizarse",
                    HttpStatus.CONFLICT);
        }
        log.info("Idempotencia: devolviendo resultado original en estado={} para clave={}", estado, request.getIdempotencyKey());
        return persistenceHelper.construirPagoResponse(existente);
    }

    private PagoResponse resolverPagoResponsePrevia(PagoResponse previa, PagoRequest request) {
        if (previa.getNumeroCuenta() != null && !Objects.equals(previa.getNumeroCuenta(), request.getNumeroCuenta())) {
            throw new CuentaException("IDEMPOTENCIA_CONFLICTO_DATOS",
                    "La clave de idempotencia ya fue utilizada con una cuenta distinta",
                    HttpStatus.CONFLICT);
        }
        if (previa.getMonto() != null && request.getMonto() != null
                && previa.getMonto().compareTo(request.getMonto()) != 0) {
            throw new CuentaException("IDEMPOTENCIA_CONFLICTO_DATOS",
                    "La clave de idempotencia ya fue utilizada con un importe distinto",
                    HttpStatus.CONFLICT);
        }
        EstadoTransaccion estado = previa.getEstado();
        if (estado == EstadoTransaccion.EN_PROCESO || estado == EstadoTransaccion.INICIADA) {
            throw new TransaccionIdempotenciaException(request.getIdempotencyKey());
        }
        if (estado == EstadoTransaccion.REVERTIDA) {
            throw new CuentaException("TRANSACCION_REVERTIDA",
                    "La clave de idempotencia pertenece a una operación revertida y no puede reutilizarse",
                    HttpStatus.CONFLICT);
        }
        log.info("Idempotencia: retornando transacción previa en estado={} con clave {}", estado, request.getIdempotencyKey());
        return previa;
    }

    /**
     * Valida en el servidor que el servicio, producto, disponibilidad, distribuidor,
     * precio/importe, UPC y referencia correspondan al catálogo oficial cacheado.
     */
    private void validarOperacionContraCatalogo(PagoRequest request) {
        if (cacheRepository == null) {
            return;
        }

        if (idDistribuidorConfigurado != null && idDistribuidorConfigurado > 0
                && !Objects.equals(request.getIdDistribuidor(), idDistribuidorConfigurado)) {
            throw new CuentaException("DISTRIBUIDOR_INVALIDO",
                    "El distribuidor solicitado no corresponde al distribuidor autorizado en el servidor",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Optional<CatalogoProductosCache> cacheOpt = cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA);
        if (cacheOpt.isEmpty() || cacheOpt.get().getProductos() == null || cacheOpt.get().getProductos().isEmpty()) {
            throw new CatalogoException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE,
                    "No se puede procesar el pago: el catálogo de productos no está disponible para verificar precios");
        }

        CatalogoProductosCache catalogo = cacheOpt.get();
        if (catalogo.getFechaActualizacion() != null
                && catalogo.getFechaActualizacion().isBefore(LocalDateTime.now().minusHours(48))) {
            throw new CatalogoException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE,
                    "No se puede procesar el pago: el catálogo de productos está desactualizado");
        }

        List<ProductoDTO> productos = catalogo.getProductos();

        boolean servicioExiste = productos.stream()
                .anyMatch(p -> Objects.equals(p.getIdServicio(), request.getIdServicio()));
        if (!servicioExiste) {
            throw new CuentaException("SERVICIO_INEXISTENTE",
                    "El servicio solicitado (" + request.getIdServicio() + ") no existe en el catálogo autorizado",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        ProductoDTO producto = productos.stream()
                .filter(p -> Objects.equals(p.getIdProducto(), request.getIdProducto()))
                .findFirst()
                .orElseThrow(() -> new CuentaException("PRODUCTO_INEXISTENTE",
                        "El producto solicitado (" + request.getIdProducto() + ") no existe en el catálogo autorizado",
                        HttpStatus.UNPROCESSABLE_ENTITY));

        if (!Objects.equals(producto.getIdServicio(), request.getIdServicio())) {
            throw new CuentaException("PRODUCTO_SERVICIO_INCOMPATIBLE",
                    String.format("El producto %s no pertenece al servicio %s",
                            request.getIdProducto(), request.getIdServicio()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new CuentaException("PRODUCTO_INACTIVO",
                    "El producto solicitado no se encuentra disponible ni habilitado para operaciones",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (producto.getPrecio() == null || producto.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CuentaException("PRECIO_CATALOGO_INVALIDO",
                    "El catálogo no contiene un precio válido verificable para el producto solicitado",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getMonto() == null || request.getMonto().compareTo(producto.getPrecio()) != 0) {
            throw new CuentaException("PRECIO_DIFERENTE_CATALOGO",
                    String.format("El importe enviado (%s) no corresponde al precio oficial del catálogo (%s)",
                            request.getMonto(), producto.getPrecio()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // El servidor impone el precio oficial del catálogo
        request.setMonto(producto.getPrecio());

        // Validar UPC si el producto del catálogo lo define
        if (producto.getUpc() != null && !producto.getUpc().isBlank()) {
            String upcCatalogo = producto.getUpc().trim();
            if (request.getUpc() != null && !request.getUpc().isBlank()
                    && !upcCatalogo.equals(request.getUpc().trim())) {
                throw new CuentaException("UPC_INVALIDO",
                        "El código UPC proporcionado no corresponde al producto del catálogo",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            request.setUpc(upcCatalogo);
        }

        // Validar reglas de referencia según el producto
        validarReferenciaSegunProducto(request.getReferencia(), producto);
    }

    private void validarReferenciaSegunProducto(String referencia, ProductoDTO producto) {
        if (referencia == null || referencia.isBlank()) {
            throw new CuentaException("REFERENCIA_INVALIDA",
                    "La referencia o número telefónico es obligatorio",
                    HttpStatus.BAD_REQUEST);
        }
        String refLimpia = referencia.trim();
        String tipoRef = producto.getTipoReferencia() != null ? producto.getTipoReferencia().trim().toLowerCase() : "a";

        boolean valida = switch (tipoRef) {
            case "t", "telefono" -> PATRON_TELEFONO_10.matcher(refLimpia).matches();
            case "n", "numerica" -> PATRON_NUMERICO.matcher(refLimpia).matches();
            default -> PATRON_REFERENCIA_ALFANUM.matcher(refLimpia).matches();
        };

        if (!valida) {
            throw new CuentaException("REFERENCIA_INVALIDA",
                    "La referencia no cumple el formato requerido por el producto (" + tipoRef + ")",
                    HttpStatus.BAD_REQUEST);
        }
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

    private boolean esFalloAutenticacionProveedor(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof FeignException.Unauthorized || cause instanceof FeignException.Forbidden) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
