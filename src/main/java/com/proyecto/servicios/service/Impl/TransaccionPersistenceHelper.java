package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.TipoOperacion;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TransaccionPersistenceHelper {

    public static final String MARCA_RESERVADA_PRE_ENVIO = "RESERVADA_PRE_ENVIO";
    public static final String MARCA_ENVIADA_PROVEEDOR = "ENVIADA_PROVEEDOR";
    public static final int MAX_INTENTOS_FALLIDOS_PIN = 3;

    private static final Pattern PATRON_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final CuentaBancariaRepository cuentaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaFisicaRepository personaFisicaRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, AtomicInteger> intentosFallidosPinPorCuenta = new ConcurrentHashMap<>();

    public TransaccionPersistenceHelper(CuentaBancariaRepository cuentaRepository,
                                        TransaccionFinancieraRepository transaccionRepository,
                                        UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder) {
        this(cuentaRepository, transaccionRepository, usuarioRepository, null, passwordEncoder);
    }

    @Autowired
    public TransaccionPersistenceHelper(CuentaBancariaRepository cuentaRepository,
                                        TransaccionFinancieraRepository transaccionRepository,
                                        UsuarioRepository usuarioRepository,
                                        PersonaFisicaRepository personaFisicaRepository,
                                        PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaFisicaRepository = personaFisicaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<PagoResponse> verificarIdempotencia(String idempotencyKey) {
        return transaccionRepository.findByIdempotencyKey(idempotencyKey)
                .map(tx -> {
                    validarPropiedadCuenta(tx.getCuenta());
                    return construirPagoResponse(tx);
                });
    }

    @Transactional(readOnly = true)
    public Optional<TransaccionFinanciera> buscarPorIdempotenciaValidandoConsistencia(PagoRequest request) {
        Optional<TransaccionFinanciera> opt = transaccionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (opt.isPresent()) {
            TransaccionFinanciera existente = opt.get();
            validarPropiedadCuenta(existente.getCuenta());
            validarConsistenciaDatosIdempotencia(existente, request);
        }
        return opt;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera iniciarYReservarSaldo(PagoRequest request) {
        // 1. Verificar si ya existe la clave de idempotencia antes de adquirir bloqueo
        Optional<TransaccionFinanciera> previaOpt = transaccionRepository.findByIdempotencyKeyForUpdate(request.getIdempotencyKey())
                .or(() -> transaccionRepository.findByIdempotencyKey(request.getIdempotencyKey()));
        if (previaOpt.isPresent()) {
            return manejarTransaccionExistente(previaOpt.get(), request);
        }

        // 2. Bloqueo pesimista de la cuenta bancaria para serializar operaciones concurrentes
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(request.getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(request.getNumeroCuenta()));

        // 3. Re-verificar idempotencia después de obtener el lock de la cuenta (protección ante carrera con la misma clave)
        Optional<TransaccionFinanciera> postLockOpt = transaccionRepository.findByIdempotencyKeyForUpdate(request.getIdempotencyKey())
                .or(() -> transaccionRepository.findByIdempotencyKey(request.getIdempotencyKey()));
        if (postLockOpt.isPresent()) {
            return manejarTransaccionExistente(postLockOpt.get(), request);
        }

        // 4. Validar propiedad de la cuenta respecto al usuario autenticado en el JWT (Anti-IDOR)
        validarPropiedadCuenta(cuenta);

        // 5. Validar estado del cliente titular y de la cuenta bancaria
        validarEstadoClienteYCuenta(cuenta);

        // 6. Validar PIN con BCrypt y control contra adivinación repetida (antes de tocar el saldo)
        validarPin(cuenta, request.getPin());

        // 7. Validar saldo suficiente (nunca permitir saldo negativo)
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CuentaException("MONTO_INVALIDO", "El monto de la operación debe ser mayor a cero", HttpStatus.BAD_REQUEST);
        }
        if (cuenta.getSaldo().compareTo(request.getMonto()) < 0) {
            throw new SaldoInsuficienteException(cuenta.getNumeroCuenta(), cuenta.getSaldo(), request.getMonto());
        }

        // 8. Máquina de estados: INICIADA -> EN_PROCESO con reserva de saldo
        EstadoTransaccion estadoInicial = EstadoTransaccion.INICIADA;
        if (!estadoInicial.puedeTransicionarA(EstadoTransaccion.EN_PROCESO)) {
            throw new IllegalStateException("Transición inválida desde " + estadoInicial);
        }

        BigDecimal nuevoSaldo = cuenta.getSaldo().subtract(request.getMonto());
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaldoInsuficienteException(cuenta.getNumeroCuenta(), cuenta.getSaldo(), request.getMonto());
        }
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);

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
                .codigoRespuestaProveedor(MARCA_RESERVADA_PRE_ENVIO)
                .mensajeProveedor("Reserva de saldo aplicada localmente")
                .build();

        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarEnviandoAProveedor(Long txId) {
        TransaccionFinanciera tx = obtenerTransaccionConLock(txId);
        if (tx.getEstado() != EstadoTransaccion.EN_PROCESO) {
            throw new CuentaException("TRANSICION_ESTADO_INVALIDA",
                    "No se puede enviar al proveedor una transacción en estado " + tx.getEstado(),
                    HttpStatus.CONFLICT);
        }
        tx.setCodigoRespuestaProveedor(MARCA_ENVIADA_PROVEEDOR);
        tx.setMensajeProveedor("Petición enviada al proveedor externo Gestopago");
        transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera liquidarExito(Long txId, GestoPagoTxResponse response) {
        TransaccionFinanciera tx = obtenerTransaccionConLock(txId);
        validarTransicionDesdeEnProceso(tx, EstadoTransaccion.EXITOSA);

        tx.setEstado(EstadoTransaccion.EXITOSA);
        tx.setCodigoRespuestaProveedor(response != null && response.getCodigo() != null ? response.getCodigo() : "0");
        tx.setMensajeProveedor(response != null && response.getMensaje() != null ? response.getMensaje() : "TRANSACCION EXITOSA");
        tx.setFolioProveedor(response != null ? response.getFolio() : null);
        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera liquidarRechazo(Long txId, GestoPagoTxResponse response) {
        TransaccionFinanciera tx = obtenerTransaccionConLock(txId);
        validarTransicionDesdeEnProceso(tx, EstadoTransaccion.RECHAZADA);

        // Reversión única y atómica del saldo reservado bajo bloqueo pesimista
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
        TransaccionFinanciera tx = obtenerTransaccionConLock(txId);
        validarTransicionDesdeEnProceso(tx, EstadoTransaccion.TIMEOUT);

        tx.setEstado(EstadoTransaccion.TIMEOUT);
        tx.setCodigoRespuestaProveedor("TIMEOUT");
        tx.setMensajeProveedor("Timeout de comunicación con Gestopago; operación en custodia para conciliación: " + detalle);
        return transaccionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera revertirPorFallaComunicacion(Long txId, String detalle) {
        TransaccionFinanciera tx = obtenerTransaccionConLock(txId);
        validarTransicionDesdeEnProceso(tx, EstadoTransaccion.RECHAZADA);

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(tx.getCuenta().getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getNumeroCuenta()));
        cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
        cuentaRepository.save(cuenta);

        tx.setEstado(EstadoTransaccion.RECHAZADA);
        tx.setCodigoRespuestaProveedor("ERROR_COMUNICACION");
        tx.setMensajeProveedor("Fallo de comunicación con Gestopago: " + detalle);
        return transaccionRepository.save(tx);
    }

    /**
     * Recupera transacciones huérfanas que quedaron en estado {@code EN_PROCESO} o {@code INICIADA}
     * tras una interrupción o caída de la aplicación entre fases:
     * <ul>
     *   <li>Si la caída ocurrió <b>antes</b> del envío HTTP ({@code RESERVADA_PRE_ENVIO}),
     *       existe certeza técnica de que el proveedor no procesó el cobro: se reembolsa el saldo
     *       y la transacción pasa a {@code REVERTIDA}.</li>
     *   <li>Si la caída ocurrió <b>después</b> de iniciar el envío HTTP ({@code ENVIADA_PROVEEDOR}),
     *       el resultado es incierto: NO se devuelve el saldo automáticamente y la transacción pasa
     *       a {@code TIMEOUT} para conciliación.</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recuperarTransaccionesPendientes(LocalDateTime umbral) {
        List<TransaccionFinanciera> pendientes = transaccionRepository.findPendientesPorEstadoYUmbral(
                EstadoTransaccion.EN_PROCESO, umbral);
        int procesadas = 0;

        for (TransaccionFinanciera candidata : pendientes) {
            TransaccionFinanciera tx = obtenerTransaccionConLock(candidata.getId());
            if (tx.getEstado() != EstadoTransaccion.EN_PROCESO) {
                continue;
            }

            if (MARCA_RESERVADA_PRE_ENVIO.equals(tx.getCodigoRespuestaProveedor())) {
                CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(tx.getCuenta().getNumeroCuenta())
                        .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getNumeroCuenta()));
                cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
                cuentaRepository.save(cuenta);

                tx.setEstado(EstadoTransaccion.REVERTIDA);
                tx.setCodigoRespuestaProveedor("REV_PRE_ENVIO");
                tx.setMensajeProveedor("Transacción revertida por recuperación: fallo previo al envío al proveedor");
                transaccionRepository.save(tx);
                log.warn("Recuperación financiera: tx folio={} revertida con reembolso por caída pre-envío", tx.getFolio());
            } else {
                tx.setEstado(EstadoTransaccion.TIMEOUT);
                tx.setCodigoRespuestaProveedor("TIMEOUT_RECUP");
                tx.setMensajeProveedor("Transacción puesta en custodia para conciliación tras interrupción posterior al envío");
                transaccionRepository.save(tx);
                log.warn("Recuperación financiera: tx folio={} marcada en TIMEOUT sin reembolso por resultado incierto", tx.getFolio());
            }
            procesadas++;
        }
        return procesadas;
    }

    /**
     * Concilia de forma controlada una transacción que se encontraba en estado {@code TIMEOUT}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionFinanciera conciliarTransaccionEnCustodia(Long txId,
                                                                boolean confirmadaExitosaEnProveedor,
                                                                String folioProveedor,
                                                                String motivoAuditoria) {
        TransaccionFinanciera tx = obtenerTransaccionConLock(txId);
        if (tx.getEstado() != EstadoTransaccion.TIMEOUT) {
            throw new CuentaException("CONCILIACION_ESTADO_INVALIDO",
                    "Solo se pueden conciliar transacciones en estado TIMEOUT (actual: " + tx.getEstado() + ")",
                    HttpStatus.CONFLICT);
        }

        if (confirmadaExitosaEnProveedor) {
            if (!tx.getEstado().puedeTransicionarA(EstadoTransaccion.EXITOSA)) {
                throw new IllegalStateException("Transición no permitida de TIMEOUT a EXITOSA");
            }
            tx.setEstado(EstadoTransaccion.EXITOSA);
            tx.setCodigoRespuestaProveedor("01");
            tx.setFolioProveedor(folioProveedor);
            tx.setMensajeProveedor("Conciliada como EXITOSA: " + motivoAuditoria);
        } else {
            if (!tx.getEstado().puedeTransicionarA(EstadoTransaccion.REVERTIDA)) {
                throw new IllegalStateException("Transición no permitida de TIMEOUT a REVERTIDA");
            }
            CuentaBancaria cuenta = cuentaRepository.findByNumeroCuentaForUpdate(tx.getCuenta().getNumeroCuenta())
                    .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getNumeroCuenta()));
            cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
            cuentaRepository.save(cuenta);

            tx.setEstado(EstadoTransaccion.REVERTIDA);
            tx.setCodigoRespuestaProveedor("REV_CONCILIACION");
            tx.setMensajeProveedor("Conciliada como REVERTIDA con restitución de saldo: " + motivoAuditoria);
        }
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

    @Transactional(readOnly = true)
    public void validarPropiedadCuenta(CuentaBancaria cuenta) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            throw new AccessDeniedException("Se requiere autenticación JWT válida para operar");
        }

        String correoAutenticado = auth.getName();
        if (correoAutenticado == null || correoAutenticado.isBlank()) {
            throw new AccessDeniedException("Identidad de usuario no disponible en el contexto de seguridad");
        }

        if (cuenta == null || cuenta.getPersonaFisica() == null) {
            throw new AccessDeniedException("La cuenta bancaria no tiene un titular válido asignado");
        }

        PersonaFisica titular = cuenta.getPersonaFisica();
        boolean coincideCorreoCliente = titular.getCorreo() != null
                && titular.getCorreo().equalsIgnoreCase(correoAutenticado.trim());

        boolean coincideCorreoUsuario = false;
        if (!coincideCorreoCliente && titular.getId() != null) {
            coincideCorreoUsuario = usuarioRepository.findByClienteId(titular.getId())
                    .map(u -> u.getCorreo() != null && u.getCorreo().equalsIgnoreCase(correoAutenticado.trim()))
                    .orElse(false);
        }

        if (!coincideCorreoCliente && !coincideCorreoUsuario) {
            log.warn("Alerta de seguridad (IDOR bloqueado): el usuario '{}' intentó acceder a la cuenta '{}'",
                    correoAutenticado, cuenta.getNumeroCuenta());
            throw new AccessDeniedException("No tiene autorización para operar o consultar recursos de otra cuenta bancaria");
        }
    }

    private TransaccionFinanciera manejarTransaccionExistente(TransaccionFinanciera existente, PagoRequest request) {
        validarPropiedadCuenta(existente.getCuenta());
        validarConsistenciaDatosIdempotencia(existente, request);

        EstadoTransaccion estado = existente.getEstado();
        if (estado == EstadoTransaccion.EN_PROCESO || estado == EstadoTransaccion.INICIADA) {
            throw new TransaccionIdempotenciaException(request.getIdempotencyKey());
        }
        if (estado == EstadoTransaccion.REVERTIDA) {
            throw new CuentaException("TRANSACCION_REVERTIDA",
                    "La clave de idempotencia pertenece a una operación revertida y no puede reutilizarse",
                    HttpStatus.CONFLICT);
        }
        log.info("Idempotencia aplicada para key={} en estado={}. Se retorna resultado original sin alterar saldo.",
                request.getIdempotencyKey(), estado);
        return existente;
    }

    public void validarConsistenciaDatosIdempotencia(TransaccionFinanciera existente, PagoRequest request) {
        String cuentaExistente = existente.getCuenta() != null ? existente.getCuenta().getNumeroCuenta() : null;
        boolean mismaCuenta = Objects.equals(cuentaExistente, request.getNumeroCuenta());
        boolean mismoMonto = existente.getMonto() != null && request.getMonto() != null
                && existente.getMonto().compareTo(request.getMonto()) == 0;
        boolean mismoServicio = Objects.equals(existente.getIdServicio(), request.getIdServicio());
        boolean mismoProducto = Objects.equals(existente.getIdProducto(), request.getIdProducto());
        boolean mismaReferencia = Objects.equals(existente.getReferencia(), request.getReferencia());

        if (!mismaCuenta || !mismoMonto || !mismoServicio || !mismoProducto || !mismaReferencia) {
            throw new CuentaException(
                    "IDEMPOTENCIA_CONFLICTO_DATOS",
                    "La clave de idempotencia ya fue utilizada con parámetros de transacción distintos",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void validarEstadoClienteYCuenta(CuentaBancaria cuenta) {
        PersonaFisica cliente = cuenta.getPersonaFisica();
        if (cliente == null) {
            throw new CuentaNoEncontradaException(cuenta.getNumeroCuenta());
        }
        if (!cliente.isActivo()) {
            throw new CuentaException("CLIENTE_INACTIVO",
                    "El cliente titular de la cuenta se encuentra inactivo",
                    HttpStatus.FORBIDDEN);
        }
        if (cliente.isLoginBloqueado()) {
            throw new CuentaException("CLIENTE_BLOQUEADO",
                    "El cliente titular tiene el acceso bloqueado",
                    HttpStatus.FORBIDDEN);
        }
        if (cliente.isCuentaBloqueada()) {
            throw new CuentaException("CUENTA_BLOQUEADA",
                    "La cuenta del cliente se encuentra bloqueada para operaciones financieras",
                    HttpStatus.FORBIDDEN);
        }
        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new CuentaInactivaException(cuenta.getNumeroCuenta(), cuenta.getEstatus());
        }
    }

    private void validarPin(CuentaBancaria cuenta, String pinIngresado) {
        String numCuenta = cuenta.getNumeroCuenta();
        AtomicInteger contadorIntentos = intentosFallidosPinPorCuenta.computeIfAbsent(numCuenta, k -> new AtomicInteger(0));
        if (contadorIntentos.get() >= MAX_INTENTOS_FALLIDOS_PIN) {
            throw new CuentaException("CUENTA_BLOQUEADA_PIN",
                    "Operación rechazada: la cuenta ha sido bloqueada por exceder el límite de intentos fallidos de PIN",
                    HttpStatus.FORBIDDEN);
        }

        if (pinIngresado == null || pinIngresado.isBlank() || "0000".equals(pinIngresado.trim())) {
            registrarFalloPin(cuenta, contadorIntentos);
            throw new PinInvalidoException();
        }

        if (cuenta.getPersonaFisica() == null || cuenta.getPersonaFisica().getId() == null) {
            throw new PinInvalidoException();
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByClienteId(cuenta.getPersonaFisica().getId());
        if (usuarioOpt.isEmpty()) {
            log.warn("Operación rechazada: el cliente id={} no posee un registro de usuario/PIN configurado",
                    cuenta.getPersonaFisica().getId());
            throw new PinInvalidoException();
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.isActivo()) {
            throw new CuentaException("USUARIO_INACTIVO",
                    "El usuario asociado a la cuenta bancaria se encuentra inactivo",
                    HttpStatus.FORBIDDEN);
        }

        String hashAlmacenado = usuario.getPassword();
        if (hashAlmacenado == null || hashAlmacenado.isBlank() || !PATRON_BCRYPT.matcher(hashAlmacenado).matches()) {
            log.error("El usuario id={} no tiene un hash BCrypt válido configurado", usuario.getId());
            throw new PinInvalidoException();
        }

        if (!passwordEncoder.matches(pinIngresado, hashAlmacenado)) {
            int fallos = registrarFalloPin(cuenta, contadorIntentos);
            if (fallos >= MAX_INTENTOS_FALLIDOS_PIN) {
                throw new CuentaException("CUENTA_BLOQUEADA_PIN",
                        "Operación rechazada: la cuenta ha sido bloqueada por exceder el límite de intentos fallidos de PIN",
                        HttpStatus.FORBIDDEN);
            }
            throw new PinInvalidoException();
        }

        intentosFallidosPinPorCuenta.remove(numCuenta);
    }

    private int registrarFalloPin(CuentaBancaria cuenta, AtomicInteger contadorIntentos) {
        int fallos = contadorIntentos.incrementAndGet();
        log.warn("Intento fallido de PIN ({}/{}) en cuenta={}", fallos, MAX_INTENTOS_FALLIDOS_PIN, cuenta.getNumeroCuenta());
        return fallos;
    }

    public void reiniciarIntentosPin(String numeroCuenta) {
        if (numeroCuenta != null) {
            intentosFallidosPinPorCuenta.remove(numeroCuenta);
        }
    }

    private TransaccionFinanciera obtenerTransaccionConLock(Long txId) {
        return transaccionRepository.findByIdForUpdate(txId)
                .or(() -> transaccionRepository.findById(txId))
                .orElseThrow(() -> new CuentaNoEncontradaException("Transacción no encontrada con ID: " + txId));
    }

    private void validarTransicionDesdeEnProceso(TransaccionFinanciera tx, EstadoTransaccion destino) {
        if (tx.getEstado() != EstadoTransaccion.EN_PROCESO || !tx.getEstado().puedeTransicionarA(destino)) {
            throw new CuentaException(
                    "TRANSICION_ESTADO_INVALIDA",
                    String.format("Transición de estado inválida para la transacción %s: %s -> %s",
                            tx.getFolio(), tx.getEstado(), destino),
                    HttpStatus.CONFLICT
            );
        }
    }
}
