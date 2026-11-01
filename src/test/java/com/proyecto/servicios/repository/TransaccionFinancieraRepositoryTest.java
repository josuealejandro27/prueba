package com.proyecto.servicios.repository;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.TipoOperacion;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoCivilRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoGenerosRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TransaccionFinancieraRepositoryTest.TestConfig.class)
class TransaccionFinancieraRepositoryTest {

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
    }

    @DynamicPropertySource
    static void datosDeConexion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:transacciones_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private TransaccionFinancieraRepository transaccionRepository;

    @Autowired
    private CuentaBancariaRepository cuentaRepository;

    @Autowired
    private PersonaFisicaRepository personaRepository;

    @Autowired
    private CatalogoGenerosRepository generosRepository;

    @Autowired
    private CatalogoPaisRepository paisRepository;

    @Autowired
    private CatalogoEstadoCivilRepository estadoCivilRepository;

    @Autowired
    private EntityManager entityManager;

    private CuentaBancaria cuenta;

    @BeforeEach
    void setUp() {
        CatalogoGeneros genero = generosRepository.save(new CatalogoGeneros(null, "Masculino"));
        CatalogoPais pais = paisRepository.save(new CatalogoPais(null, "Mexico", null));
        CatalogoEstadoCivil estadoCivil = estadoCivilRepository.save(new CatalogoEstadoCivil(null, "Soltero"));

        PersonaFisica persona = new PersonaFisica();
        persona.setNombre("Pedro");
        persona.setApellidoPaterno("Lopez");
        persona.setApellidoMaterno("Hernandez");
        persona.setFechaNacimiento(java.sql.Date.valueOf("1988-05-12"));
        persona.setCurp("LOHP880512HDFRRN01");
        persona.setRfc("LOHP880512AB1");
        persona.setGenero(genero);
        persona.setNacionalidad(pais);
        persona.setEstadoCivil(estadoCivil);
        persona.setCorreo("pedro.lopez@example.com");
        persona.setLada((short) 55);
        persona.setNumeroTelefono(5598765432L);
        persona.setCuentaBloqueada(false);
        persona.setLoginBloqueado(false);
        persona.setActivo(true);
        persona = personaRepository.save(persona);

        cuenta = new CuentaBancaria();
        cuenta.setNumeroCuenta("1234567890123456");
        cuenta.setSaldo(new BigDecimal("500.00"));
        cuenta.setFechaApertura(LocalDateTime.now());
        cuenta.setPersonaFisica(persona);
        cuenta.setEstatus("ACTIVA");
        cuenta = cuentaRepository.save(cuenta);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Persistir transacción financiera: guarda correctamente con folio y estado inicial")
    void persistirTransaccionFinanciera() {
        CuentaBancaria cuentaRef = cuentaRepository.findById(cuenta.getId()).orElseThrow();

        String folio = UUID.randomUUID().toString();
        String idempotencyKey = "key-" + UUID.randomUUID();

        TransaccionFinanciera tx = TransaccionFinanciera.builder()
                .folio(folio)
                .idempotencyKey(idempotencyKey)
                .cuenta(cuentaRef)
                .monto(new BigDecimal("99.90"))
                .tipoOperacion(TipoOperacion.PAGO_SERVICIO)
                .estado(EstadoTransaccion.INICIADA)
                .idDistribuidor(83)
                .idServicio(105L)
                .idProducto(201L)
                .referencia("5512345678")
                .build();

        TransaccionFinanciera guardada = transaccionRepository.save(tx);
        entityManager.flush();
        entityManager.clear();

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getFechaActualizacion()).isNotNull();

        Optional<TransaccionFinanciera> recuperada = transaccionRepository.findById(guardada.getId());
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getFolio()).isEqualTo(folio);
        assertThat(recuperada.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(recuperada.get().getEstado()).isEqualTo(EstadoTransaccion.INICIADA);
        assertThat(recuperada.get().getMonto()).isEqualByComparingTo("99.90");
    }

    @Test
    @DisplayName("Idempotencia estricta: clave duplicada lanza DataIntegrityViolationException")
    void claveIdempotenciaDuplicadaEsRechazada() {
        CuentaBancaria cuentaRef = cuentaRepository.findById(cuenta.getId()).orElseThrow();
        String claveRepetida = "idemp-dup-key-12345";

        TransaccionFinanciera tx1 = TransaccionFinanciera.builder()
                .folio(UUID.randomUUID().toString())
                .idempotencyKey(claveRepetida)
                .cuenta(cuentaRef)
                .monto(new BigDecimal("50.00"))
                .tipoOperacion(TipoOperacion.RECARGA_TIEMPO_AIRE)
                .estado(EstadoTransaccion.INICIADA)
                .idDistribuidor(83)
                .referencia("5511223344")
                .build();

        transaccionRepository.save(tx1);
        entityManager.flush();

        TransaccionFinanciera tx2 = TransaccionFinanciera.builder()
                .folio(UUID.randomUUID().toString())
                .idempotencyKey(claveRepetida) // Misma clave
                .cuenta(cuentaRef)
                .monto(new BigDecimal("50.00"))
                .tipoOperacion(TipoOperacion.RECARGA_TIEMPO_AIRE)
                .estado(EstadoTransaccion.INICIADA)
                .idDistribuidor(83)
                .referencia("5511223344")
                .build();

        assertThatThrownBy(() -> {
            transaccionRepository.save(tx2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Bloqueo pesimista: findByNumeroCuentaForUpdate adquiere y retorna la cuenta")
    void bloqueoPesimistaDeCuenta() {
        Optional<CuentaBancaria> bloqueada = cuentaRepository.findByNumeroCuentaForUpdate("1234567890123456");

        assertThat(bloqueada).isPresent();
        assertThat(bloqueada.get().getSaldo()).isEqualByComparingTo("500.00");
        assertThat(bloqueada.get().getEstatus()).isEqualTo("ACTIVA");

        // Simular débito en la cuenta
        CuentaBancaria c = bloqueada.get();
        c.setSaldo(c.getSaldo().subtract(new BigDecimal("100.00")));
        cuentaRepository.save(c);
        entityManager.flush();
        entityManager.clear();

        CuentaBancaria actualizada = cuentaRepository.findByNumeroCuenta("1234567890123456").orElseThrow();
        assertThat(actualizada.getSaldo()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Consultas de repositorio: buscar por idempotencyKey, folio y estado")
    void consultasDeTransacciones() {
        CuentaBancaria cuentaRef = cuentaRepository.findById(cuenta.getId()).orElseThrow();
        String folio = UUID.randomUUID().toString();
        String idempKey = "query-key-xyz";

        TransaccionFinanciera tx = TransaccionFinanciera.builder()
                .folio(folio)
                .idempotencyKey(idempKey)
                .cuenta(cuentaRef)
                .monto(new BigDecimal("150.00"))
                .tipoOperacion(TipoOperacion.PAGO_SERVICIO)
                .estado(EstadoTransaccion.EXITOSA)
                .idDistribuidor(83)
                .referencia("REC-987654")
                .codigoRespuestaProveedor("0")
                .mensajeProveedor("TRANSACCION EXITOSA")
                .folioProveedor("AUT-998877")
                .build();

        transaccionRepository.save(tx);
        entityManager.flush();
        entityManager.clear();

        assertThat(transaccionRepository.existsByIdempotencyKey(idempKey)).isTrue();
        assertThat(transaccionRepository.findByIdempotencyKey(idempKey)).isPresent();
        assertThat(transaccionRepository.findByFolio(folio)).isPresent();

        List<TransaccionFinanciera> porEstado = transaccionRepository.findByEstado(EstadoTransaccion.EXITOSA);
        assertThat(porEstado).hasSize(1);
        assertThat(porEstado.get(0).getCodigoRespuestaProveedor()).isEqualTo("0");

        List<TransaccionFinanciera> porCuenta = transaccionRepository.findByCuentaIdOrderByFechaCreacionDesc(cuentaRef.getId());
        assertThat(porCuenta).hasSize(1);
    }
}

