package com.proyecto.servicios.e2e;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.PinInvalidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TransaccionConcurrenciaYCasosLimiteTest.TestConfig.class)
@Import(TransaccionPersistenceHelper.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Pruebas de concurrencia, resiliencia y casos límite financieros")
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
    }

    @AfterEach
    void tearDown() {
        transaccionRepository.deleteAll();
        cuentaRepository.deleteAll();
        usuarioRepository.deleteAll();
        personaRepository.deleteAll();
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
    @DisplayName("Prevención de doble gasto concurrente: con $150 de saldo, 2 cobros simultáneos de $100 solo aprueban 1")
    void prevencionDeDobleGastoConcurrente() throws Exception {
        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger rechazadosPorSaldo = new AtomicInteger(0);

        Callable<Void> tarea1 = () -> {
            latch.await();
            try {
                persistenceHelper.iniciarYReservarSaldo(crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("100.00"), "idemp-conc-1"));
                exitos.incrementAndGet();
            } catch (SaldoInsuficienteException e) {
                rechazadosPorSaldo.incrementAndGet();
            } catch (Exception e) {
                // Otro error si compite
            }
            return null;
        };

        Callable<Void> tarea2 = () -> {
            latch.await();
            try {
                persistenceHelper.iniciarYReservarSaldo(crearRequest(cuenta.getNumeroCuenta(), new BigDecimal("100.00"), "idemp-conc-2"));
                exitos.incrementAndGet();
            } catch (SaldoInsuficienteException e) {
                rechazadosPorSaldo.incrementAndGet();
            } catch (Exception e) {
                // Otro error si compite
            }
            return null;
        };

        Future<Void> f1 = executor.submit(tarea1);
        Future<Void> f2 = executor.submit(tarea2);

        // Disparar simultáneamente
        latch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactamente 1 debió tener éxito reservando $100, y el otro debió fallar por saldo insuficiente
        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazadosPorSaldo.get()).isEqualTo(1);

        CuentaBancaria cuentaActualizada = cuentaRepository.findByNumeroCuenta(cuenta.getNumeroCuenta()).orElseThrow();
        // El saldo debe ser exactamente $50.00 (150 - 100), jamás negativo
        assertThat(cuentaActualizada.getSaldo()).isEqualByComparingTo("50.00");
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

        // Simular respuesta con código y mensaje nulos
        GestoPagoTxResponse responseVacia = new GestoPagoTxResponse();
        responseVacia.setCodigo(null);
        responseVacia.setMensaje(null);

        TransaccionFinanciera rechazada = persistenceHelper.liquidarRechazo(tx.getId(), responseVacia);

        assertThat(rechazada.getEstado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(rechazada.getCodigoRespuestaProveedor()).isEqualTo("RECHAZADO");

        // El saldo debió devolverse intacto a $150.00
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
