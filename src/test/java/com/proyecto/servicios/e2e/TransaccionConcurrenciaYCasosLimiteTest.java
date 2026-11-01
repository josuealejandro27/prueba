package com.proyecto.servicios.e2e;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CuentaException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionIdempotenciaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoCivilRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoGenerosRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import com.proyecto.servicios.service.Impl.TransaccionPersistenceHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TransaccionConcurrenciaYCasosLimiteTest.TestConfig.class)
@Import(TransaccionPersistenceHelper.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Pruebas de concurrencia, máquina de estados, recuperación y casos límite financieros")
class TransaccionConcurrenciaYCasosLimiteTest {

    @Configuration
    @EnableJpaRepositories(basePackages = {
            "com.proyecto.servicios.repositorys.clientes",
            "com.proyecto.servicios.repositorys.catalogos",
            "com.proyecto.servicios.repositorys.transacciones"})
    @EntityScan(basePackages = {
            "com.proyecto.servicios.entity.clientes",
            "com.proyecto.servicios.entity.catalogos",
            "com.proyecto.servicios.entity.transacciones"})
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @DynamicPropertySource
    static void datosDeConexion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:conc_limite_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private TransaccionPersistenceHelper persistenceHelper;

    @Autowired
    private CuentaBancariaRepository cuentaRepository;

    @Autowired
    private TransaccionFinancieraRepository transaccionRepository;

    @Autowired
    private PersonaFisicaRepository personaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CatalogoGenerosRepository generosRepository;

    @Autowired
    private CatalogoPaisRepository paisRepository;

    @Autowired
    private CatalogoEstadoCivilRepository estadoCivilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CuentaBancaria cuenta;
    private PersonaFisica persona;
    private Usuario usuario;

    private static final AtomicInteger COUNTER = new AtomicInteger(10);

    @BeforeEach
    void setUp() {
        CatalogoGeneros genero = generosRepository.save(new CatalogoGeneros(null, "Femenino"));
        CatalogoPais pais = paisRepository.save(new CatalogoPais(null, "Mexico", null));
        CatalogoEstadoCivil estadoCivil = estadoCivilRepository.save(new CatalogoEstadoCivil(null, "Casado"));

        int seq = COUNTER.incrementAndGet();
        String idDosDigitos = String.format("%02d", (seq % 90) + 10);
        String cuenta16 = "9999" + String.format("%012d", seq);

        persona = new PersonaFisica();
        persona.setNombre("Maria");
        persona.setApellidoPaterno("Sanchez");
        persona.setApellidoMaterno("Ramirez");
        persona.setFechaNacimiento(java.sql.Date.valueOf("1992-03-15"));
        persona.setCurp("SARM920315MDFRRN" + idDosDigitos);
        persona.setRfc("SARM920315CD" + (seq % 10));
        persona.setGenero(genero);
        persona.setNacionalidad(pais);
        persona.setEstadoCivil(estadoCivil);
        persona.setCorreo("maria.sanchez-" + seq + "@example.com");
        persona.setLada((short) 55);
        persona.setNumeroTelefono(5588776655L);
        persona.setCuentaBloqueada(false);
        persona.setLoginBloqueado(false);
        persona.setActivo(true);
        persona = personaRepository.save(persona);

        usuario = new Usuario();
        usuario.setCliente(persona);
        usuario.setCorreo(persona.getCorreo());
        usuario.setPassword(passwordEncoder.encode("PinSeguro123!"));
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        cuenta = new CuentaBancaria();
        cuenta.setNumeroCuenta(cuenta16);
        cuenta.setSaldo(new BigDecimal("150.00"));
        cuenta.setFechaApertura(LocalDateTime.now());
        cuenta.setPersonaFisica(persona);
        cuenta.setEstatus("ACTIVA");
        cuenta = cuentaRepository.save(cuenta);

        persistenceHelper.reiniciarIntentosPin(cuenta.getNumeroCuenta());
        autenticarEnContexto(persona.getCorreo());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transaccionRepository.deleteAll();
        cuentaRepository.deleteAll();
        usuarioRepository.deleteAll();
        personaRepository.deleteAll();
    }

    private void autenticarEnContexto(String correo) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, Collections.emptyList()));
    }

    private PagoRequest crearRequest(String numeroCuenta, BigDecimal monto, String idempKey) {
        return PagoRequest.builder()
                .numeroCuenta(numeroCuenta)
                .idServicio(105L)
                .idProducto(201L)
                .monto(monto)
                .idDistribuidor(83)
                .referencia("5512345678")
                .bandera("T")
                .pin("PinSeguro123!")
                .idempotencyKey(idempKey)
                .build();
    }

    @Test
    @DisplayName("Prevención de doble gasto concurrente (claves distintas): con $150 de saldo, 2 cobros simultáneos de $100 solo aprueban 1")
    void prevencionDeDobleGastoConcurrente() throws Exception {
        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);
        final String correoTitular = persona.getCorreo();

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger rechazadosPorSaldo = new AtomicInteger(0);

        Callable<Void> tarea1 = () -> {
            autenticarEnContexto(correoTitular);
            latch.await();
            try {
                persistenceHelper.iniciarYReservarSaldo(crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("100.00"), "idemp-conc-1"));
                exitos.incrementAndGet();
            } catch (SaldoInsuficienteException e) {
                rechazadosPorSaldo.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Callable<Void> tarea2 = () -> {
            autenticarEnContexto(correoTitular);
            latch.await();
            try {
                persistenceHelper.iniciarYReservarSaldo(crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("100.00"), "idemp-conc-2"));
                exitos.incrementAndGet();
            } catch (SaldoInsuficienteException e) {
                rechazadosPorSaldo.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(tarea1);
        Future<Void> f2 = executor.submit(tarea2);

        latch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazadosPorSaldo.get()).isEqualTo(1);

        CuentaBancaria cuentaActualizada = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(cuentaActualizada.getSaldo()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Dos solicitudes simultáneas con la MISMA clave de idempotencia: solo 1 reserva saldo y la otra es bloqueada con 409")
    void dosSolicitudesSimultaneasConMismaClaveIdempotencia() throws Exception {
        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);
        final String correoTitular = persona.getCorreo();
        final String claveCompartida = "idemp-misma-clave-simultanea";

        AtomicInteger reservadas = new AtomicInteger(0);
        AtomicInteger bloqueadasPorIdempotencia = new AtomicInteger(0);

        Callable<Void> tarea = () -> {
            autenticarEnContexto(correoTitular);
            latch.await();
            try {
                persistenceHelper.iniciarYReservarSaldo(crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), claveCompartida));
                reservadas.incrementAndGet();
            } catch (TransaccionIdempotenciaException e) {
                bloqueadasPorIdempotencia.incrementAndGet();
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                bloqueadasPorIdempotencia.incrementAndGet();
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(tarea);
        Future<Void> f2 = executor.submit(tarea);

        latch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(reservadas.get()).isEqualTo(1);
        assertThat(bloqueadasPorIdempotencia.get()).isEqualTo(1);

        // El saldo solo se descontó una vez (150 - 50 = 100)
        CuentaBancaria enBd = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(enBd.getSaldo()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Prevención de doble reembolso: liquidarRechazo sobre transacción ya RECHAZADA lanza excepción y no altera el saldo")
    void prevencionDeDobleReembolsoEnLiquidarRechazo() {
        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-no-doble-reembolso"));

        GestoPagoTxResponse rechazo = GestoPagoTxResponse.builder().codigo("80").mensaje("RECHAZADA").build();
        persistenceHelper.liquidarRechazo(tx.getId(), rechazo);

        CuentaBancaria trasPrimerRechazo = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(trasPrimerRechazo.getSaldo()).isEqualByComparingTo("150.00");

        // Intentar liquidarRechazo una segunda vez debe ser bloqueado por la máquina de estados
        assertThatThrownBy(() -> persistenceHelper.liquidarRechazo(tx.getId(), rechazo))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");

        // El saldo jamás debe inflarse a $200.00
        CuentaBancaria trasSegundoIntento = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(trasSegundoIntento.getSaldo()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Prevención de pago gratuito: una transacción RECHAZADA no puede transicionar después a EXITOSA")
    void prevencionDePagoGratuitoDesdeRechazadaAExitosa() {
        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-no-pago-gratis"));

        GestoPagoTxResponse rechazo = GestoPagoTxResponse.builder().codigo("80").mensaje("RECHAZADA").build();
        persistenceHelper.liquidarRechazo(tx.getId(), rechazo);

        GestoPagoTxResponse exito = GestoPagoTxResponse.builder().codigo("01").mensaje("APROBADA").build();
        assertThatThrownBy(() -> persistenceHelper.liquidarExito(tx.getId(), exito))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    @DisplayName("Conflicto de datos en idempotencia: reutilizar la misma clave con otro monto o referencia es rechazado")
    void idempotenciaConDatosDiferentesEsRechazada() {
        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-conflicto-datos"));
        persistenceHelper.liquidarExito(tx.getId(), GestoPagoTxResponse.builder().codigo("01").build());

        PagoRequest requestManipulado = crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("90.00"), "idemp-conflicto-datos");

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(requestManipulado))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("IDEMPOTENCIA_CONFLICTO_DATOS");
    }

    @Test
    @DisplayName("Caída de la aplicación entre fases (pre-envío vs post-envío) y recuperación automática controlada")
    void caidaEntreFasesYRecuperacionDeTransaccionesPendientes() {
        // 1. Transacción que cayó ANTES de enviarse al proveedor (RESERVADA_PRE_ENVIO) -> debe revertirse y devolver saldo
        TransaccionFinanciera txPreEnvio = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("40.00"), "idemp-caida-pre-envio"));

        // 2. Transacción que cayó DESPUÉS de enviarse al proveedor (ENVIADA_PROVEEDOR) -> debe pasar a TIMEOUT sin devolver saldo
        TransaccionFinanciera txPostEnvio = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("60.00"), "idemp-caida-post-envio"));
        persistenceHelper.marcarEnviandoAProveedor(txPostEnvio.getId());

        // Saldo tras reservar ambas (150 - 40 - 60 = 50)
        assertThat(cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow().getSaldo())
                .isEqualByComparingTo("50.00");

        // Ejecutar recuperación de pendientes
        int recuperadas = persistenceHelper.recuperarTransaccionesPendientes(LocalDateTime.now().plusSeconds(5));
        assertThat(recuperadas).isEqualTo(2);

        TransaccionFinanciera txPreActualizada = transaccionRepository.findById(txPreEnvio.getId()).orElseThrow();
        TransaccionFinanciera txPostActualizada = transaccionRepository.findById(txPostEnvio.getId()).orElseThrow();

        assertThat(txPreActualizada.getEstado()).isEqualTo(EstadoTransaccion.REVERTIDA);
        assertThat(txPostActualizada.getEstado()).isEqualTo(EstadoTransaccion.TIMEOUT);

        // El saldo debió recuperar únicamente los $40 de la pre-envío (50 + 40 = 90); los $60 de la post-envío siguen en custodia
        assertThat(cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow().getSaldo())
                .isEqualByComparingTo("90.00");

        // Ahora conciliar la transacción en TIMEOUT confirmando que NO fue cobrada en el proveedor -> pasa a REVERTIDA y devuelve los $60
        TransaccionFinanciera conciliada = persistenceHelper.conciliarTransaccionEnCustodia(
                txPostEnvio.getId(), false, null, "Confirmado no procesado por mesa de control");
        assertThat(conciliada.getEstado()).isEqualTo(EstadoTransaccion.REVERTIDA);
        assertThat(cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow().getSaldo())
                .isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("PIN inexistente (cliente sin registro en tabla usuario): rechaza operación y jamás acepta por ausencia de fila")
    void pinInexistenteClienteSinUsuarioEsRechazado() {
        usuarioRepository.delete(usuario);

        PagoRequest req = crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("20.00"), "idemp-sin-usuario");

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(req))
                .isInstanceOf(PinInvalidoException.class);

        // Tampoco acepta "0000"
        req.setPin("0000");
        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(req))
                .isInstanceOf(PinInvalidoException.class);

        assertThat(cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow().getSaldo())
                .isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Protección contra fuerza bruta de PIN: bloquea la cuenta tras alcanzar el límite de intentos fallidos")
    void fuerzaBrutaPinBloqueaOperaciones() {
        PagoRequest reqMalo = crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("10.00"), "idemp-pin-bruteforce");
        reqMalo.setPin("PinErroneo1");

        // Intento 1 -> PinInvalidoException
        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(reqMalo))
                .isInstanceOf(PinInvalidoException.class);

        // Intento 2 -> PinInvalidoException
        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(reqMalo))
                .isInstanceOf(PinInvalidoException.class);

        // Intento 3 -> Alcanza el máximo y bloquea la cuenta con CUENTA_BLOQUEADA_PIN
        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(reqMalo))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("CUENTA_BLOQUEADA_PIN");

        // Incluso si ahora envía el PIN correcto, sigue bloqueada
        PagoRequest reqConPinCorrecto = crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("10.00"), "idemp-pin-despues-bloq");
        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(reqConPinCorrecto))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("CUENTA_BLOQUEADA_PIN");
    }

    @Test
    @DisplayName("Anti-IDOR: usuario autenticado distinto al titular no puede operar sobre cuenta ajena")
    void usuarioIntentaOperarConCuentaAjenaEsRechazado() {
        autenticarEnContexto("otro.usuario.atacante@example.com");

        PagoRequest req = crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-idor-cuenta-ajena");

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(req))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow().getSaldo())
                .isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Saldo exacto: cuenta con $100.00 y cobro de $100.00 deja el saldo en exactamente $0.00")
    void cobroConSaldoExacto() {
        cuenta.setSaldo(new BigDecimal("100.00"));
        cuentaRepository.save(cuenta);

        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("100.00"), "idemp-exacto-001"));

        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.EN_PROCESO);

        CuentaBancaria enBd = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(enBd.getSaldo()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Saldo cero: cuenta con $0.00 y cobro de $1.00 lanza SaldoInsuficienteException")
    void cobroConSaldoCeroEsRechazado() {
        cuenta.setSaldo(BigDecimal.ZERO);
        cuentaRepository.save(cuenta);

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("1.00"), "idemp-cero-001")))
                .isInstanceOf(SaldoInsuficienteException.class);

        CuentaBancaria enBd = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(enBd.getSaldo()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Cuenta inactiva: cuenta BLOQUEADA rechaza el cobro antes de tocar el saldo")
    void cuentaBloqueadaEsRechazada() {
        cuenta.setEstatus("BLOQUEADA");
        cuentaRepository.save(cuenta);

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-bloq-001")))
                .isInstanceOf(CuentaInactivaException.class);
    }

    @Test
    @DisplayName("Cliente con cuentaBloqueada=true o inactivo es rechazado")
    void clienteBloqueadoOInactivoEsRechazado() {
        persona.setCuentaBloqueada(true);
        personaRepository.save(persona);

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-cli-bloq")))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("CUENTA_BLOQUEADA");
    }

    @Test
    @DisplayName("PIN incorrecto: rechaza la transacción y no afecta el saldo de la cuenta")
    void pinIncorrectoEsRechazado() {
        PagoRequest requestConPinMalo = crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-pin-malo");
        requestConPinMalo.setPin("ContrasenaTotalmenteErronea!");

        assertThatThrownBy(() -> persistenceHelper.iniciarYReservarSaldo(requestConPinMalo))
                .isInstanceOf(PinInvalidoException.class);

        CuentaBancaria enBd = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(enBd.getSaldo()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Resiliencia ante respuestas inesperadas del proveedor: nulls no causan NPE y reversan el saldo")
    void respuestaInesperadaProveedorReversaSaldo() {
        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-null-res"));

        CuentaBancaria retenida = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(retenida.getSaldo()).isEqualByComparingTo("100.00");

        GestoPagoTxResponse responseVacia = new GestoPagoTxResponse();
        responseVacia.setCodigo(null);
        responseVacia.setMensaje(null);

        TransaccionFinanciera rechazada = persistenceHelper.liquidarRechazo(tx.getId(), responseVacia);

        assertThat(rechazada.getEstado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(rechazada.getCodigoRespuestaProveedor()).isEqualTo("RECHAZADO");

        CuentaBancaria restaurada = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        assertThat(restaurada.getSaldo()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Idempotencia: consulta de clave existente devuelve el estado previo")
    void verificacionIdempotenciaRetornaTransaccionPrevia() {
        TransaccionFinanciera tx = persistenceHelper.iniciarYReservarSaldo(
                crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("50.00"), "idemp-previa-001"));

        GestoPagoTxResponse exito = GestoPagoTxResponse.builder().codigo("0").mensaje("OK").build();
        persistenceHelper.liquidarExito(tx.getId(), exito);

        Optional<PagoResponse> previa = persistenceHelper.verificarIdempotencia("idemp-previa-001");
        assertThat(previa).isPresent();
        assertThat(previa.get().getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(previa.get().getMonto()).isEqualByComparingTo("50.00");
    }
}
