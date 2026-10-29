package com.proyecto.servicios.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoCivilRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoGenerosRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.repositorys.mongo.CatalogoProductosCacheRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Pruebas de integración End-to-End del flujo completo financiero")
class FlujoCompletoPagoEndToEndTest {

    private static HttpServer mockServer;
    private static int mockPort;

    // Control de comportamiento del servidor mock de Gestopago
    // 0 = Exito (200), 1 = Rechazo de negocio (200 con codigo 80), 2 = Error 500, 3 = Timeout, 4 = Respuesta malformada
    private static final AtomicInteger MODO_MOCK = new AtomicInteger(0);
    private static final AtomicInteger LLAMADAS_TX = new AtomicInteger(0);
    private static final AtomicReference<String> ULTIMO_AUTH_HEADER = new AtomicReference<>();
    private static final AtomicReference<String> ULTIMO_BODY = new AtomicReference<>();

    @BeforeAll
    static void iniciarServidorMock() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockServer.getAddress().getPort();

        // Mock para sendTx.do de Gestopago
        mockServer.createContext("/sistema/service/sendTx.do", exchange -> {
            LLAMADAS_TX.incrementAndGet();
            ULTIMO_AUTH_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ULTIMO_BODY.set(body);

            int modo = MODO_MOCK.get();
            if (modo == 1) {
                // Rechazo de negocio con codigo 80
                responder(exchange, 200, "{\"codigo\":\"80\",\"mensaje\":\"TELEFONO O CUENTA DESTINO INVALIDA\"}");
            } else if (modo == 2) {
                // Error 500 del proveedor
                responder(exchange, 500, "{\"message\":\"Error interno temporal en plataforma Gestopago\"}");
            } else if (modo == 3) {
                // Timeout simulado (>1500 ms)
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                responder(exchange, 200, "{\"codigo\":\"00\",\"mensaje\":\"TARDE\",\"folio\":\"GP-TIMEOUT\"}");
            } else if (modo == 4) {
                // Respuesta corrupta / malformada
                responder(exchange, 200, "{\"codigo\": corrupt_json_sin_comillas");
            } else {
                // Exito 200
                responder(exchange, 200, "{\"codigo\":\"00\",\"mensaje\":\"TRANSACCION EXITOSA\",\"folio\":\"GP-789012\"}");
            }
        });

        // Mock para autenticacion de token Gestopago
        mockServer.createContext("/sistema/app/jwt-gp/authenticate/", exchange ->
                responder(exchange, 200, "{\"token\":\"token-gestopago-mock\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

        mockServer.start();
    }

    @AfterAll
    static void detenerServidorMock() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    private static void responder(HttpExchange exchange, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @DynamicPropertySource
    static void configurarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("gestopago.tx.url", () -> "http://localhost:" + mockPort);
        registry.add("gestopago.tx.ruta", () -> "/sistema/service/sendTx.do");
        registry.add("gestopago.tx.aes.key", () -> "PruebaClaveSecretaGestopago256Bit!");
        registry.add("gestopago.tx.aes.iv", () -> "1234567890123456");
        registry.add("feign.client.config.gestoPagoTx.connectTimeout", () -> "500");
        registry.add("feign.client.config.gestoPagoTx.readTimeout", () -> "500");

        registry.add("gestopago.auth.url", () -> "http://localhost:" + mockPort);
        registry.add("gestopago.auth.id-distribuidor", () -> "999");
        registry.add("gestopago.auth.codigo-dispositivo", () -> "TEST-DEV-E2E");

        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:flujo_pago_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

        registry.add("jwt.secret", () -> "clave_secreta_jwt_para_pruebas_e2e_de_al_menos_32_bytes_12345");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration_test");

        registry.add("spring.data.mongodb.database", () -> "flujo_cache_e2e_test");
        registry.add("spring.data.mongodb.uri", () -> "mongodb://127.0.0.1:1/flujo_cache_e2e_test");
    }

    @TestConfiguration
    static class TestMongoConfig {
        private final java.util.Map<String, CatalogoProductosCache> store = new ConcurrentHashMap<>();

        @Bean
        @Primary
        public CatalogoProductosCacheRepository testCatalogoProductosCacheRepository() {
            CatalogoProductosCacheRepository mockRepo = org.mockito.Mockito.mock(CatalogoProductosCacheRepository.class);
            org.mockito.Mockito.when(mockRepo.save(org.mockito.Mockito.any(CatalogoProductosCache.class)))
                    .thenAnswer(inv -> {
                        CatalogoProductosCache c = inv.getArgument(0);
                        store.put(c.getId(), c);
                        return c;
                    });
            org.mockito.Mockito.when(mockRepo.findById(org.mockito.Mockito.anyString()))
                    .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0, String.class))));
            return mockRepo;
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    private GestoPagoTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final AtomicInteger COUNTER = new AtomicInteger(100);
    private String jwtToken;
    private String numeroCuentaValida;
    private final String PIN_CORRECTO = "PinPrueba123!";

    @BeforeEach
    void setUp() throws Exception {
        MODO_MOCK.set(0);
        LLAMADAS_TX.set(0);
        ULTIMO_AUTH_HEADER.set(null);
        ULTIMO_BODY.set(null);

        // Sembrar token Bearer activo en BD para Gestopago
        tokenRepository.deleteAll();
        GestoPagoToken gpToken = new GestoPagoToken();
        gpToken.setIdDistribuidor(999);
        gpToken.setCodigoDispositivo("TEST-DEV-E2E");
        gpToken.setToken("token-gestopago-mock");
        gpToken.setTokenType("Bearer");
        gpToken.setExpiresIn(3600L);
        gpToken.setActivo(true);
        tokenRepository.save(gpToken);

        // Crear cliente, usuario y cuenta para las pruebas
        int seq = COUNTER.incrementAndGet();
        String idDosDigitos = String.format("%02d", (seq % 90) + 10);
        numeroCuentaValida = "4444" + String.format("%012d", seq);

        CatalogoGeneros genero = generosRepository.save(new CatalogoGeneros(null, "Masculino"));
        CatalogoPais pais = paisRepository.save(new CatalogoPais(null, "Mexico", null));
        CatalogoEstadoCivil estadoCivil = estadoCivilRepository.save(new CatalogoEstadoCivil(null, "Soltero"));

        PersonaFisica persona = new PersonaFisica();
        persona.setNombre("Carlos");
        persona.setApellidoPaterno("Hernandez");
        persona.setApellidoMaterno("Lopez");
        persona.setFechaNacimiento(java.sql.Date.valueOf("1995-07-20"));
        persona.setCurp("HELC950720HDFRRN" + idDosDigitos);
        persona.setRfc("HELC950720AB" + (seq % 10));
        persona.setGenero(genero);
        persona.setNacionalidad(pais);
        persona.setEstadoCivil(estadoCivil);
        persona.setCorreo("carlos.e2e." + seq + "@example.com");
        persona.setLada((short) 55);
        persona.setNumeroTelefono(5512345670L + (seq % 9));
        persona.setCuentaBloqueada(false);
        persona.setLoginBloqueado(false);
        persona.setActivo(true);
        persona = personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setCliente(persona);
        usuario.setCorreo(persona.getCorreo());
        usuario.setPassword(passwordEncoder.encode(PIN_CORRECTO));
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setNumeroCuenta(numeroCuentaValida);
        cuenta.setSaldo(new BigDecimal("500.00"));
        cuenta.setFechaApertura(LocalDateTime.now());
        cuenta.setPersonaFisica(persona);
        cuenta.setEstatus("ACTIVA");
        cuentaRepository.save(cuenta);

        // Iniciar sesión para obtener el JWT
        LoginRequest loginReq = new LoginRequest();
        loginReq.setCorreo(persona.getCorreo());
        loginReq.setPassword(PIN_CORRECTO);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginReq, headers);

        ResponseEntity<String> loginResp = restTemplate.postForEntity("/auth/login", requestEntity, String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode root = objectMapper.readTree(loginResp.getBody());
        jwtToken = root.path("data").path("token").asText();
        assertThat(jwtToken).isNotBlank();
    }

    private HttpHeaders headersAutenticados() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwtToken);
        return headers;
    }

    private PagoRequest crearPagoValido(BigDecimal monto, String idempKey) {
        return PagoRequest.builder()
                .numeroCuenta(numeroCuentaValida)
                .idServicio(105L)
                .idProducto(201L)
                .monto(monto)
                .idDistribuidor(999)
                .referencia("5588990011")
                .bandera("T")
                .pin(PIN_CORRECTO)
                .idempotencyKey(idempKey)
                .build();
    }

    @Test
    @DisplayName("Escenario 1: Flujo completo exitoso End-to-End (Auth -> Pago 200 -> Débito -> Gestopago Mock -> Consulta)")
    void flujoCompletoExitosoEndToEnd() throws Exception {
        MODO_MOCK.set(0);
        String idempKey = "e2e-exito-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("150.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(respuesta.getBody());
        assertThat(json.path("code").asText()).isEqualTo("200");
        assertThat(json.path("data").path("estado").asText()).isEqualTo("EXITOSA");
        assertThat(json.path("data").path("folio").asText()).isNotBlank();
        assertThat(json.path("data").path("folioProveedor").asText()).isEqualTo("GP-789012");
        assertThat(new BigDecimal(json.path("data").path("saldoRestante").asText())).isEqualByComparingTo("350.00");

        // Validar que el mock de Gestopago recibio el token Bearer y la peticion
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);
        assertThat(ULTIMO_AUTH_HEADER.get()).contains("Bearer token-gestopago-mock");
        assertThat(ULTIMO_BODY.get()).contains("\"firma\":");

        // Validar persistencia real en BD
        CuentaBancaria cuentaActualizada = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuentaActualizada.getSaldo()).isEqualByComparingTo("350.00");

        TransaccionFinanciera tx = transaccionRepository.findByIdempotencyKey(idempKey).orElseThrow();
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(tx.getFolioProveedor()).isEqualTo("GP-789012");

        // Validar consulta posterior vía POST /transacciones/consultar
        ConsultaTransaccionRequest consultaReq = ConsultaTransaccionRequest.builder()
                .folio(idempKey)
                .build();
        HttpEntity<ConsultaTransaccionRequest> consultaEntity = new HttpEntity<>(consultaReq, headersAutenticados());
        ResponseEntity<String> respConsulta = restTemplate.postForEntity("/transacciones/consultar", consultaEntity, String.class);

        assertThat(respConsulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode jsonConsulta = objectMapper.readTree(respConsulta.getBody());
        assertThat(jsonConsulta.path("data").path("estado").asText()).isEqualTo("EXITOSA");
        assertThat(jsonConsulta.path("data").path("folio").asText()).isNotBlank();
        assertThat(jsonConsulta.path("data").path("folioProveedor").asText()).isEqualTo("GP-789012");
    }

    @Test
    @DisplayName("Escenario 2: Idempotencia End-to-End (Reintento con misma clave devuelve el estado previo sin duplicar cobro)")
    void idempotenciaEndToEndNoDuplicaCobro() throws Exception {
        MODO_MOCK.set(0);
        String idempKey = "e2e-idemp-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("100.00"), idempKey);

        // Primera llamada
        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> primeraResp = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);
        assertThat(primeraResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        // Segunda llamada con exactamente la misma idempotencyKey
        ResponseEntity<String> segundaResp = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);
        assertThat(segundaResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // No debió haber una segunda llamada al proveedor externo
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        // El saldo debió descontarse una sola vez (500 - 100 = 400)
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Escenario 3: Rechazo de negocio por el proveedor (Código 80 revierte íntegramente el saldo)")
    void rechazoNegocioProveedorRevierteSaldo() throws Exception {
        MODO_MOCK.set(1); // Modo rechazo: codigo 80
        String idempKey = "e2e-rechazo-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("120.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        JsonNode json = objectMapper.readTree(respuesta.getBody());
        assertThat(json.path("code").asText()).isEqualTo("TRANSACCION_RECHAZADA");
        assertThat(json.path("data").path("estado").asText()).isEqualTo("RECHAZADA");
        assertThat(json.path("data").path("codigoRespuestaProveedor").asText()).isEqualTo("80");
        assertThat(json.path("data").path("mensajeProveedor").asText()).contains("TELEFONO O CUENTA DESTINO INVALIDA");

        // El saldo debió ser revertido y quedar en $500.00 exactos
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");

        TransaccionFinanciera tx = transaccionRepository.findByIdempotencyKey(idempKey).orElseThrow();
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.RECHAZADA);
    }

    @Test
    @DisplayName("Escenario 4: Error HTTP 500 del proveedor revierte automáticamente el saldo")
    void error500ProveedorRevierteSaldo() throws Exception {
        MODO_MOCK.set(2); // Modo error 500
        String idempKey = "e2e-err500-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("80.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        JsonNode json = objectMapper.readTree(respuesta.getBody());
        assertThat(json.path("data").path("estado").asText()).isEqualTo("RECHAZADA");

        // Saldo restaurado íntegramente
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 5: Timeout con el proveedor marca estado y custodia sin corromper saldo")
    void timeoutProveedorManejaEstadoAdecuado() throws Exception {
        MODO_MOCK.set(3); // Modo timeout (demora 2000 ms cuando timeout está en 1000 ms)
        String idempKey = "e2e-timeout-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("70.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        JsonNode json = objectMapper.readTree(respuesta.getBody());
        assertThat(json.path("code").asText()).isEqualTo("TIMEOUT");
        assertThat(json.path("data").path("estado").asText()).isEqualTo("TIMEOUT");

        TransaccionFinanciera tx = transaccionRepository.findByIdempotencyKey(idempKey).orElseThrow();
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.TIMEOUT);
    }

    @Test
    @DisplayName("Escenario 6: Respuesta malformada del proveedor revierte saldo sin lanzar 500 no controlado")
    void respuestaMalformadaRevierteSaldo() throws Exception {
        MODO_MOCK.set(4); // Modo respuesta corrupta
        String idempKey = "e2e-malform-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("90.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 7: Bean Validation rechaza petición con datos inválidos sin tocar proveedor ni saldo")
    void beanValidationRechazaPeticionInvalida() throws Exception {
        PagoRequest requestInvalido = crearPagoValido(new BigDecimal("-50.00"), "e2e-negativo"); // Monto negativo

        HttpEntity<PagoRequest> entity = new HttpEntity<>(requestInvalido, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // No se llamó al proveedor
        assertThat(LLAMADAS_TX.get()).isEqualTo(0);

        // Saldo intacto
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 8: PIN incorrecto rechaza la operación sin invocar a Gestopago")
    void pinIncorrectoRechazaSinTocarProveedor() throws Exception {
        PagoRequest requestPinMalo = crearPagoValido(new BigDecimal("100.00"), "e2e-pin-malo");
        requestPinMalo.setPin("PinTotalmenteErroneo!");

        HttpEntity<PagoRequest> entity = new HttpEntity<>(requestPinMalo, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Proveedor no invocado
        assertThat(LLAMADAS_TX.get()).isEqualTo(0);

        // Saldo intacto
        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 9: Petición sin token JWT es rechazada por el filtro de seguridad (401)")
    void peticionSinJwtEsRechazada() {
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("100.00"), "e2e-sin-jwt");

        HttpHeaders headersSinJwt = new HttpHeaders();
        headersSinJwt.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersSinJwt);

        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

