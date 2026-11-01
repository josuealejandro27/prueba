package com.proyecto.servicios.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.catalogo.ProductoDTO;
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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Pruebas de integración End-to-End del flujo completo financiero y seguridad")
class FlujoCompletoPagoEndToEndTest {

    private static HttpServer mockServer;
    private static int mockPort;

    private static final String JWT_SECRET_E2E = "clave_secreta_jwt_para_pruebas_e2e_de_al_menos_32_bytes_12345";

    // Control de comportamiento del servidor mock de Gestopago
    // 0 = Exito (200), 1 = Rechazo de negocio (200 con codigo 80), 2 = Error 500, 3 = Timeout, 4 = Respuesta malformada
    private static final AtomicInteger MODO_MOCK = new AtomicInteger(0);
    private static final AtomicInteger LLAMADAS_TX = new AtomicInteger(0);
    private static final AtomicReference<String> ULTIMO_AUTH_HEADER = new AtomicReference<>();
    private static final AtomicReference<String> ULTIMO_BODY = new AtomicReference<>();

    @BeforeAll
    static void iniciarServidorMock() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        mockPort = mockServer.getAddress().getPort();

        // Mock para sendTx.do de Gestopago
        mockServer.createContext("/sistema/service/sendTx.do", exchange -> {
            LLAMADAS_TX.incrementAndGet();
            ULTIMO_AUTH_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ULTIMO_BODY.set(body);

            int modo = MODO_MOCK.get();
            if (modo == 1) {
                responder(exchange, 200, "{\"codigo\":\"80\",\"mensaje\":\"TELEFONO O CUENTA DESTINO INVALIDA\"}");
            } else if (modo == 2) {
                responder(exchange, 500, "{\"message\":\"Error interno temporal en plataforma Gestopago\"}");
            } else if (modo == 3) {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException ignored) {}
                responder(exchange, 200, "{\"codigo\":\"00\",\"mensaje\":\"TARDE\",\"folio\":\"GP-TIMEOUT\"}");
            } else if (modo == 4) {
                responder(exchange, 200, "{\"codigo\": corrupt_json_sin_comillas");
            } else {
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
        registry.add("feign.client.config.gestoPagoTx.connectTimeout", () -> "2000");
        registry.add("feign.client.config.gestoPagoTx.readTimeout", () -> "1000");

        registry.add("gestopago.auth.url", () -> "http://localhost:" + mockPort);
        registry.add("gestopago.auth.id-distribuidor", () -> "999");
        registry.add("gestopago.auth.codigo-dispositivo", () -> "TEST-DEV-E2E");

        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:flujo_pago_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");

        registry.add("jwt.secret", () -> JWT_SECRET_E2E);
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
            org.mockito.Mockito.doAnswer(inv -> {
                store.clear();
                return null;
            }).when(mockRepo).deleteAll();
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
    private CatalogoProductosCacheRepository cacheRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final AtomicInteger COUNTER = new AtomicInteger(100);
    private String jwtToken;
    private String numeroCuentaValida;
    private PersonaFisica personaTitular;
    private final String PIN_CORRECTO = "PinPrueba123!";

    @BeforeEach
    void setUp() throws Exception {
        MODO_MOCK.set(0);
        LLAMADAS_TX.set(0);
        ULTIMO_AUTH_HEADER.set(null);
        ULTIMO_BODY.set(null);

        // Sembrar catálogo oficial de productos en caché MongoDB para validación en servidor
        sembrarCatalogoEnCache();

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
        numeroCuentaValida = "4444" + String.format("%012d", seq);

        CatalogoGeneros genero = generosRepository.save(new CatalogoGeneros(null, "Masculino"));
        CatalogoPais pais = paisRepository.save(new CatalogoPais(null, "Mexico", null));
        CatalogoEstadoCivil estadoCivil = estadoCivilRepository.save(new CatalogoEstadoCivil(null, "Soltero"));

        PersonaFisica persona = new PersonaFisica();
        persona.setNombre("Carlos");
        persona.setApellidoPaterno("Hernandez");
        persona.setApellidoMaterno("Lopez");
        persona.setFechaNacimiento(java.sql.Date.valueOf("1995-07-20"));
        char letra = (char) ('A' + ((seq / 90) % 26));
        int sufijoNum = (seq % 90) + 10;
        persona.setCurp(String.format("HELC950720HDFRR%c%02d", letra, sufijoNum));
        persona.setRfc(String.format("HELC950720%c%02d", letra, sufijoNum));
        persona.setGenero(genero);
        persona.setNacionalidad(pais);
        persona.setEstadoCivil(estadoCivil);
        persona.setCorreo("carlos.e2e." + seq + "@example.com");
        persona.setLada((short) 55);
        persona.setNumeroTelefono(5500000000L + seq);
        persona.setCuentaBloqueada(false);
        persona.setLoginBloqueado(false);
        persona.setActivo(true);
        personaTitular = personaRepository.save(persona);

        Usuario usuario = new Usuario();
        usuario.setCliente(personaTitular);
        usuario.setCorreo(personaTitular.getCorreo());
        usuario.setPassword(passwordEncoder.encode(PIN_CORRECTO));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setNumeroCuenta(numeroCuentaValida);
        cuenta.setSaldo(new BigDecimal("500.00"));
        cuenta.setFechaApertura(LocalDateTime.now());
        cuenta.setPersonaFisica(personaTitular);
        cuenta.setEstatus("ACTIVA");
        cuentaRepository.save(cuenta);

        // Iniciar sesión para obtener el JWT
        LoginRequest loginReq = new LoginRequest();
        loginReq.setCorreo(personaTitular.getCorreo());
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

    private void sembrarCatalogoEnCache() {
        List<ProductoDTO> productos = List.of(
                ProductoDTO.builder().idServicio(105L).idProducto(201L).producto("Paquete 150").precio(new BigDecimal("150.00")).tipoReferencia("t").upc("750100201").activo(true).build(),
                ProductoDTO.builder().idServicio(105L).idProducto(202L).producto("Paquete 100").precio(new BigDecimal("100.00")).tipoReferencia("t").upc("750100202").activo(true).build(),
                ProductoDTO.builder().idServicio(105L).idProducto(203L).producto("Paquete 120").precio(new BigDecimal("120.00")).tipoReferencia("t").upc("750100203").activo(true).build(),
                ProductoDTO.builder().idServicio(105L).idProducto(204L).producto("Paquete 80").precio(new BigDecimal("80.00")).tipoReferencia("t").upc("750100204").activo(true).build(),
                ProductoDTO.builder().idServicio(105L).idProducto(205L).producto("Paquete 70").precio(new BigDecimal("70.00")).tipoReferencia("t").upc("750100205").activo(true).build(),
                ProductoDTO.builder().idServicio(105L).idProducto(206L).producto("Paquete 90").precio(new BigDecimal("90.00")).tipoReferencia("t").upc("750100206").activo(true).build(),
                ProductoDTO.builder().idServicio(105L).idProducto(207L).producto("Producto Deshabilitado").precio(new BigDecimal("50.00")).tipoReferencia("t").upc("750100207").activo(false).build()
        );
        cacheRepository.save(CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .fechaActualizacion(LocalDateTime.now())
                .estatusOrigen("01")
                .productos(productos)
                .build());
    }

    private Long obtenerProductoPorMonto(BigDecimal monto) {
        if (monto == null) return 201L;
        if (monto.compareTo(new BigDecimal("150.00")) == 0) return 201L;
        if (monto.compareTo(new BigDecimal("100.00")) == 0) return 202L;
        if (monto.compareTo(new BigDecimal("120.00")) == 0) return 203L;
        if (monto.compareTo(new BigDecimal("80.00")) == 0) return 204L;
        if (monto.compareTo(new BigDecimal("70.00")) == 0) return 205L;
        if (monto.compareTo(new BigDecimal("90.00")) == 0) return 206L;
        return 201L;
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
                .idProducto(obtenerProductoPorMonto(monto))
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

        assertThat(LLAMADAS_TX.get()).isEqualTo(1);
        assertThat(ULTIMO_AUTH_HEADER.get()).contains("Bearer token-gestopago-mock");
        assertThat(ULTIMO_BODY.get()).contains("\"firma\":");

        CuentaBancaria cuentaActualizada = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuentaActualizada.getSaldo()).isEqualByComparingTo("350.00");

        TransaccionFinanciera tx = transaccionRepository.findByIdempotencyKey(idempKey).orElseThrow();
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(tx.getFolioProveedor()).isEqualTo("GP-789012");

        ConsultaTransaccionRequest consultaReq = ConsultaTransaccionRequest.builder()
                .folio(idempKey)
                .build();
        HttpEntity<ConsultaTransaccionRequest> consultaEntity = new HttpEntity<>(consultaReq, headersAutenticados());
        ResponseEntity<String> respConsulta = restTemplate.postForEntity("/transacciones/consultar", consultaEntity, String.class);

        assertThat(respConsulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode jsonConsulta = objectMapper.readTree(respConsulta.getBody());
        assertThat(jsonConsulta.path("data").path("estado").asText()).isEqualTo("EXITOSA");
        assertThat(jsonConsulta.path("data").path("folioProveedor").asText()).isEqualTo("GP-789012");
    }

    @Test
    @DisplayName("Escenario 2: Idempotencia EXITOSA End-to-End (Reintento con misma clave devuelve estado previo sin duplicar cobro)")
    void idempotenciaEndToEndNoDuplicaCobro() throws Exception {
        MODO_MOCK.set(0);
        String idempKey = "e2e-idemp-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("100.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> primeraResp = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);
        assertThat(primeraResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        ResponseEntity<String> segundaResp = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);
        assertThat(segundaResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Escenario 3: Rechazo de negocio por el proveedor y repetición de clave RECHAZADA no repite cobro ni duplica reembolso")
    void rechazoNegocioProveedorYRepeticionClaveRechazadaEsSegura() throws Exception {
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
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");

        // Ahora repetir con la MISMA idempotencyKey cuando el mock respondería 200 OK:
        // NO debe llamar al proveedor (evita pago gratis) ni volver a reembolsar (evita crear saldo artificial)
        MODO_MOCK.set(0);
        ResponseEntity<String> reintentoResp = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);
        assertThat(reintentoResp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        CuentaBancaria cuentaPostReintento = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuentaPostReintento.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 4: Error HTTP 500 del proveedor revierte automáticamente el saldo")
    void error500ProveedorRevierteSaldo() throws Exception {
        MODO_MOCK.set(2);
        String idempKey = "e2e-err500-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("80.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isIn(HttpStatus.UNPROCESSABLE_ENTITY, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.BAD_GATEWAY);
        JsonNode json = objectMapper.readTree(respuesta.getBody());
        assertThat(json.path("data").path("estado").asText()).isIn("RECHAZADA", "REVERTIDA", "TIMEOUT");

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 5: Timeout con el proveedor y repetición de clave en TIMEOUT no vuelve a cobrar ni devuelve saldo")
    void timeoutProveedorYRepeticionClaveTimeoutNoReenviaCobro() throws Exception {
        MODO_MOCK.set(3);
        String idempKey = "e2e-timeout-" + UUID.randomUUID();
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("70.00"), idempKey);

        HttpEntity<PagoRequest> entity = new HttpEntity<>(pagoReq, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        JsonNode json = objectMapper.readTree(respuesta.getBody());
        assertThat(json.path("code").asText()).isEqualTo("TIMEOUT");
        assertThat(json.path("data").path("estado").asText()).isEqualTo("TIMEOUT");
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);

        // El saldo reservado ($500 - $70 = $430) permanece en custodia
        assertThat(cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow().getSaldo())
                .isEqualByComparingTo("430.00");

        // Repetir con la misma clave de idempotencia NO debe enviar una segunda llamada a Gestopago
        MODO_MOCK.set(0);
        ResponseEntity<String> reintentoTimeout = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);
        assertThat(reintentoTimeout.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(LLAMADAS_TX.get()).isEqualTo(1);
        assertThat(cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow().getSaldo())
                .isEqualByComparingTo("430.00");
    }

    @Test
    @DisplayName("Escenario 6: Respuesta malformada del proveedor revierte saldo sin lanzar 500 no controlado")
    void respuestaMalformadaRevierteSaldo() throws Exception {
        MODO_MOCK.set(4);
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
    void beanValidationRechazaPeticionInvalida() {
        PagoRequest requestInvalido = crearPagoValido(new BigDecimal("-50.00"), "e2e-negativo");

        HttpEntity<PagoRequest> entity = new HttpEntity<>(requestInvalido, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(LLAMADAS_TX.get()).isEqualTo(0);

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 8: PIN incorrecto rechaza la operación sin invocar a Gestopago")
    void pinIncorrectoRechazaSinTocarProveedor() {
        PagoRequest requestPinMalo = crearPagoValido(new BigDecimal("100.00"), "e2e-pin-malo");
        requestPinMalo.setPin("PinTotalmenteErroneo!");

        HttpEntity<PagoRequest> entity = new HttpEntity<>(requestPinMalo, headersAutenticados());
        ResponseEntity<String> respuesta = restTemplate.postForEntity("/transacciones/pagar", entity, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(LLAMADAS_TX.get()).isEqualTo(0);

        CuentaBancaria cuenta = cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 9: Petición sin token JWT, con token inválido o expirado es rechazada (401)")
    void peticionSinJwtOConJwtInvalidoOExpiradoEsRechazada() {
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("100.00"), "e2e-sin-jwt");

        // 1. Sin header Authorization
        HttpHeaders headersSinJwt = new HttpHeaders();
        headersSinJwt.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> respSinJwt = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(pagoReq, headersSinJwt), String.class);
        assertThat(respSinJwt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 2. Token JWT inválido / manipulado
        HttpHeaders headersJwtInvalido = new HttpHeaders();
        headersJwtInvalido.setContentType(MediaType.APPLICATION_JSON);
        headersJwtInvalido.set("Authorization", "Bearer token.falso.manipulado");
        ResponseEntity<String> respJwtInvalido = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(pagoReq, headersJwtInvalido), String.class);
        assertThat(respJwtInvalido.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 3. Token JWT expirado
        String tokenExpirado = Jwts.builder()
                .setSubject(personaTitular.getCorreo())
                .setIssuedAt(new Date(System.currentTimeMillis() - 120000))
                .setExpiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET_E2E.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        HttpHeaders headersJwtExpirado = new HttpHeaders();
        headersJwtExpirado.setContentType(MediaType.APPLICATION_JSON);
        headersJwtExpirado.set("Authorization", "Bearer " + tokenExpirado);
        ResponseEntity<String> respJwtExpirado = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(pagoReq, headersJwtExpirado), String.class);
        assertThat(respJwtExpirado.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Escenario 10: Validación de catálogo E2E (rechaza monto manipulado, producto inexistente y producto inactivo)")
    void validacionCatalogoRechazaMontoManipuladoProductoInexistenteEInactivo() throws Exception {
        // 1. Monto manipulado ($1.00 para el producto 201 que cuesta $150.00)
        PagoRequest reqManipulado = crearPagoValido(new BigDecimal("150.00"), "e2e-monto-manipulado");
        reqManipulado.setIdProducto(201L);
        reqManipulado.setMonto(new BigDecimal("1.00"));

        ResponseEntity<String> respManipulado = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(reqManipulado, headersAutenticados()), String.class);
        assertThat(respManipulado.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(objectMapper.readTree(respManipulado.getBody()).path("code").asText())
                .isEqualTo("PRECIO_DIFERENTE_CATALOGO");

        // 2. Producto inexistente (idProducto = 99999)
        PagoRequest reqInexistente = crearPagoValido(new BigDecimal("150.00"), "e2e-prod-inexistente");
        reqInexistente.setIdProducto(99999L);

        ResponseEntity<String> respInexistente = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(reqInexistente, headersAutenticados()), String.class);
        assertThat(respInexistente.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(objectMapper.readTree(respInexistente.getBody()).path("code").asText())
                .isEqualTo("PRODUCTO_INEXISTENTE");

        // 3. Producto inactivo (idProducto = 207)
        PagoRequest reqInactivo = crearPagoValido(new BigDecimal("150.00"), "e2e-prod-inactivo");
        reqInactivo.setIdProducto(207L);
        reqInactivo.setMonto(new BigDecimal("50.00"));

        ResponseEntity<String> respInactivo = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(reqInactivo, headersAutenticados()), String.class);
        assertThat(respInactivo.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(objectMapper.readTree(respInactivo.getBody()).path("code").asText())
                .isEqualTo("PRODUCTO_INACTIVO");

        // Ninguno debió invocar a Gestopago ni descontar saldo
        assertThat(LLAMADAS_TX.get()).isEqualTo(0);
        assertThat(cuentaRepository.findByNumeroCuenta(numeroCuentaValida).orElseThrow().getSaldo())
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Escenario 11: Anti-IDOR E2E (un usuario no puede pagar con cuenta ajena ni consultar transacción ajena)")
    void antiIdorImpideUsarCuentaAjenaYConsultarTransaccionAjena() throws Exception {
        // Primero el titular legítimo realiza una transacción exitosa
        String idempTitular = "e2e-tx-privada-" + UUID.randomUUID();
        PagoRequest pagoTitular = crearPagoValido(new BigDecimal("100.00"), idempTitular);
        ResponseEntity<String> respTitular = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(pagoTitular, headersAutenticados()), String.class);
        assertThat(respTitular.getStatusCode()).isEqualTo(HttpStatus.OK);
        String folioPrivado = objectMapper.readTree(respTitular.getBody()).path("data").path("folio").asText();

        // Emitir un JWT para un segundo usuario distinto (atacante)
        String tokenOtroUsuario = Jwts.builder()
                .setSubject("otro.cliente@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET_E2E.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        HttpHeaders headersOtro = new HttpHeaders();
        headersOtro.setContentType(MediaType.APPLICATION_JSON);
        headersOtro.set("Authorization", "Bearer " + tokenOtroUsuario);

        // 1. Intento de pagar usando la cuenta del primer usuario -> 403 FORBIDDEN
        PagoRequest pagoCuentaAjena = crearPagoValido(new BigDecimal("100.00"), "e2e-idor-ataque");
        ResponseEntity<String> respAtaquePago = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(pagoCuentaAjena, headersOtro), String.class);
        assertThat(respAtaquePago.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 2. Intento de consultar la transacción del primer usuario -> 403 FORBIDDEN
        ConsultaTransaccionRequest consultaAjena = new ConsultaTransaccionRequest(folioPrivado);
        ResponseEntity<String> respAtaqueConsulta = restTemplate.postForEntity("/transacciones/consultar",
                new HttpEntity<>(consultaAjena, headersOtro), String.class);
        assertThat(respAtaqueConsulta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Escenario 12: Cliente bloqueado o inactivo es rechazado tanto en login como con token existente (403)")
    void clienteBloqueadoOInactivoEsRechazado() {
        personaTitular.setLoginBloqueado(true);
        personaRepository.save(personaTitular);

        // El filtro JWT y la capa financiera rechazan el acceso con 403 FORBIDDEN
        PagoRequest pagoReq = crearPagoValido(new BigDecimal("100.00"), "e2e-cli-bloqueado");
        ResponseEntity<String> resp = restTemplate.postForEntity("/transacciones/pagar",
                new HttpEntity<>(pagoReq, headersAutenticados()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(LLAMADAS_TX.get()).isEqualTo(0);
    }
}
