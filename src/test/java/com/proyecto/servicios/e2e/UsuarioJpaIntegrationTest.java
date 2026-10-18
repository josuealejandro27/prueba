package com.proyecto.servicios.e2e;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.model.clientes.UsuarioUpdateRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoCivilRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoGenerosRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración real de la administración de usuarios sobre una base de datos SQL
 * (H2 en modo PostgreSQL por defecto; se puede apuntar a PostgreSQL local sobrescribiendo
 * las propiedades del sistema {@code test.db.*}).
 *
 * <p>Ejerce el {@link UsuarioServiceImpl} y el {@link UsuarioRepository} reales: persistencia JPA,
 * transacciones, consultas {@code Specification} combinadas, restricciones de la BD y
 * protección de los campos controlados por el servidor. Las filas se revierten al final de
 * cada prueba ({@code @DataJpaTest} es transaccional), por lo que no se altera ningún dato real.</p>
 *
 * <p>Ejecución contra PostgreSQL local:</p>
 * <pre>
 * docker run ... -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres
 * docker exec &lt;contenedor&gt; createdb -U postgres usuarios_e2e
 * ./gradlew test --tests "*UsuarioJpaIntegrationTest" \
 *   -Dtest.db.url=jdbc:postgresql://localhost:5432/usuarios_e2e \
 *   -Dtest.db.user=postgres -Dtest.db.password=postgres \
 *   -Dtest.db.driver=org.postgresql.Driver \
 *   -Dtest.db.dialect=org.hibernate.dialect.PostgreSQLDialect
 * </pre>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UsuarioJpaIntegrationTest.TestConfig.class)
@Import(UsuarioServiceImpl.class)
class UsuarioJpaIntegrationTest {

    @Configuration
    @EnableJpaRepositories(basePackages = {
            "com.proyecto.servicios.repositorys.clientes",
            "com.proyecto.servicios.repositorys.catalogos"})
    @EntityScan(basePackages = {
            "com.proyecto.servicios.entity.clientes",
            "com.proyecto.servicios.entity.catalogos"})
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @DynamicPropertySource
    static void datosDeConexion(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("test.db.url",
                "jdbc:h2:mem:usuarios_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE"));
        registry.add("spring.datasource.username", () -> System.getProperty("test.db.user", "sa"));
        registry.add("spring.datasource.password", () -> System.getProperty("test.db.password", ""));
        registry.add("spring.datasource.driver-class-name", () -> System.getProperty("test.db.driver", "org.h2.Driver"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> System.getProperty("test.db.dialect",
                "org.hibernate.dialect.H2Dialect"));
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private UsuarioServiceImpl usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaFisicaRepository personaFisicaRepository;

    @Autowired
    private CatalogoGenerosRepository catalogoGenerosRepository;

    @Autowired
    private CatalogoPaisRepository catalogoPaisRepository;

    @Autowired
    private CatalogoEstadoCivilRepository catalogoEstadoCivilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private CatalogoGeneros genero;
    private CatalogoPais pais;
    private CatalogoEstadoCivil estadoCivil;

    @BeforeEach
    void setUp() {
        genero = catalogoGenerosRepository.save(new CatalogoGeneros(null, "Masculino"));
        pais = catalogoPaisRepository.save(new CatalogoPais(null, "Mexico", null));
        estadoCivil = catalogoEstadoCivilRepository.save(new CatalogoEstadoCivil(null, "Soltero"));
    }

    private PersonaFisica nuevoCliente(String sufijo) {
        String id = String.format("%02d", Math.abs(sufijo.hashCode()) % 100);
        PersonaFisica persona = new PersonaFisica();
        persona.setNombre("Juan");
        persona.setApellidoPaterno("Perez");
        persona.setApellidoMaterno("Garcia");
        persona.setFechaNacimiento(java.sql.Date.valueOf("1990-01-01"));
        persona.setCurp("PEGJ900101HDFRRN" + id);
        persona.setRfc("PEGJ900101AB" + id.charAt(1));
        persona.setGenero(genero);
        persona.setNacionalidad(pais);
        persona.setEstadoCivil(estadoCivil);
        persona.setCorreo("cliente-" + UUID.randomUUID() + "@example.com");
        persona.setLada((short) 55);
        persona.setNumeroTelefono(5512345678L);
        persona.setCuentaBloqueada(false);
        persona.setLoginBloqueado(false);
        persona.setActivo(true);
        return personaFisicaRepository.save(persona);
    }

    private UsuarioCreateRequest requestAlta(Long clienteId, String correo, String password) {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(clienteId);
        request.setCorreo(correo);
        request.setPassword(password);
        return request;
    }

    private void limpiarPersistencia() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Alta de usuario: persiste, cifra la contraseña y fija los campos del servidor")
    void altaPersisteYCifraLaContrasena() {
        PersonaFisica cliente = nuevoCliente("alta");

        UsuarioResponse response = usuarioService.crearUsuario(
                requestAlta(cliente.getId(), "juan.perez@example.com", "Password123!"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCorreo()).isEqualTo("juan.perez@example.com");
        assertThat(response.isActivo()).isTrue();
        assertThat(response.getFechaCreacion()).isNotNull();
        assertThat(response.getFechaActualizacion()).isNotNull();

        limpiarPersistencia();
        Usuario enBd = usuarioRepository.findById(response.getId()).orElseThrow();

        assertThat(enBd.isActivo()).as("activo se fija en el servidor").isTrue();
        assertThat(enBd.getPassword()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", enBd.getPassword()))
                .as("la contraseña debe estar cifrada con BCrypt").isTrue();
        assertThat(enBd.getFechaCreacion()).as("fechaCreacion asignada por JPA").isNotNull();
        assertThat(enBd.getFechaActualizacion()).as("fechaActualizacion asignada por JPA").isNotNull();
    }

    @Test
    @DisplayName("Correo duplicado en otro cliente: la BD/servicio responde 409 CONFLICT")
    void correoDuplicadoEsRechazado() {
        PersonaFisica clienteA = nuevoCliente("dupA");
        PersonaFisica clienteB = nuevoCliente("dupB");
        String correo = "repetido@example.com";

        usuarioService.crearUsuario(requestAlta(clienteA.getId(), correo, "Password123!"));

        assertThatThrownBy(() -> usuarioService.crearUsuario(requestAlta(clienteB.getId(), correo, "Password123!")))
                .isInstanceOf(UsuarioException.class)
                .satisfies(ex -> assertThat(((UsuarioException) ex).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Un cliente solo puede tener un usuario: segundo alta responde 409 CONFLICT")
    void clienteConUsuarioExistenteEsRechazado() {
        PersonaFisica cliente = nuevoCliente("unico");

        usuarioService.crearUsuario(requestAlta(cliente.getId(), "uno@example.com", "Password123!"));

        assertThatThrownBy(() -> usuarioService.crearUsuario(requestAlta(cliente.getId(), "dos@example.com", "Password123!")))
                .isInstanceOf(UsuarioException.class)
                .satisfies(ex -> assertThat(((UsuarioException) ex).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Actualizar no modifica activo ni fechaCreacion (campos del servidor intactos)")
    void actualizarNoAlteraCamposDelServidor() {
        PersonaFisica cliente = nuevoCliente("upd");
        UsuarioResponse creado = usuarioService.crearUsuario(
                requestAlta(cliente.getId(), "antes@example.com", "Password123!"));

        usuarioService.desactivarUsuario(creado.getId());
        limpiarPersistencia();
        Date creacionOriginal = usuarioRepository.findById(creado.getId()).orElseThrow().getFechaCreacion();

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(creado.getId());
        request.setCorreo("despues@example.com");
        request.setPassword("NuevaPass1!");
        usuarioService.actualizarUsuario(request);

        limpiarPersistencia();
        Usuario enBd = usuarioRepository.findById(creado.getId()).orElseThrow();

        assertThat(enBd.isActivo()).as("actualizar no debe reactivar").isFalse();
        assertThat(enBd.getCorreo()).isEqualTo("despues@example.com");
        assertThat(enBd.getFechaCreacion()).isEqualTo(creacionOriginal);
        assertThat(passwordEncoder.matches("NuevaPass1!", enBd.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Desactivar y activar cambian el estado sin tocar fechaCreacion")
    void desactivarYActivarCambianEstado() {
        PersonaFisica cliente = nuevoCliente("estado");
        UsuarioResponse creado = usuarioService.crearUsuario(
                requestAlta(cliente.getId(), "estado@example.com", "Password123!"));

        limpiarPersistencia();
        Date creacionOriginal = usuarioRepository.findById(creado.getId()).orElseThrow().getFechaCreacion();

        assertThat(usuarioService.desactivarUsuario(creado.getId()).isActivo()).isFalse();
        assertThat(usuarioService.activarUsuario(creado.getId()).isActivo()).isTrue();

        limpiarPersistencia();
        Usuario enBd = usuarioRepository.findById(creado.getId()).orElseThrow();
        assertThat(enBd.isActivo()).isTrue();
        assertThat(enBd.getFechaCreacion()).isEqualTo(creacionOriginal);
    }

    @Test
    @DisplayName("Búsqueda combinada: correo parcial + clienteId + activos en una sola consulta")
    void busquedaCombinadaDeFiltros() {
        PersonaFisica clienteA = nuevoCliente("busA");
        PersonaFisica clienteB = nuevoCliente("busB");

        usuarioService.crearUsuario(requestAlta(clienteA.getId(), "ana.garcia@example.com", "Password123!"));
        UsuarioResponse usuarioB = usuarioService.crearUsuario(
                requestAlta(clienteB.getId(), "luis.garcia@example.com", "Password123!"));
        usuarioService.crearUsuario(requestAlta(
                nuevoCliente("busC").getId(), "ana.otro@example.com", "Password123!"));
        usuarioService.desactivarUsuario(usuarioB.getId());

        // Solo correo (parcial, sin distinguir mayúsculas)
        UsuarioSearchRequest porCorreo = new UsuarioSearchRequest();
        porCorreo.setCorreo("GARCIA");
        assertThat(usuarioService.buscarUsuarios(porCorreo).getContent())
                .extracting(UsuarioResponse::getCorreo)
                .containsExactlyInAnyOrder("ana.garcia@example.com", "luis.garcia@example.com");

        // Combinado: correo + clienteId + activos
        UsuarioSearchRequest combinado = new UsuarioSearchRequest();
        combinado.setCorreo("garcia");
        combinado.setClienteId(clienteA.getId());
        combinado.setActivos(true);
        assertThat(usuarioService.buscarUsuarios(combinado).getContent())
                .extracting(UsuarioResponse::getCorreo)
                .containsExactly("ana.garcia@example.com");

        // Sin filtros: devuelve todos
        assertThat(usuarioService.buscarUsuarios(new UsuarioSearchRequest()).getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Usuario inexistente: obtener y actualizar responden 404 NOT_FOUND")
    void usuarioInexistenteRespondeNoEncontrado() {
        assertThatThrownBy(() -> usuarioService.obtenerUsuario(999999L))
                .isInstanceOf(UsuarioException.class)
                .satisfies(ex -> assertThat(((UsuarioException) ex).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(999999L);
        request.setCorreo("nadie@example.com");
        assertThatThrownBy(() -> usuarioService.actualizarUsuario(request))
                .isInstanceOf(UsuarioException.class)
                .satisfies(ex -> assertThat(((UsuarioException) ex).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
