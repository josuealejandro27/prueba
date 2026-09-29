package com.proyecto.servicios;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.read.ListAppender;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.model.catalogo.ProductoDTO;
import com.proyecto.servicios.repositorys.mongo.CatalogoProductosCacheRepository;
import com.proyecto.servicios.service.ProductoCatalogoService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba end-to-end de la integración del catálogo de productos:
 * <ul>
 *   <li>Servicio externo simulado (mock HTTP) consumido vía OpenFeign.</li>
 *   <li>Bearer Token resuelto desde PostgreSQL (no desde properties).</li>
 *   <li>Política de reintentos + alerta crítica.</li>
 *   <li>Caché en MongoDB y consulta vía endpoint local.</li>
 * </ul>
 * Requiere PostgreSQL (5432) y MongoDB (27017) disponibles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogoProductoEndToEndTest {

    private static final String TOKEN_PRUEBA = "Bearer TOKEN_E2E_123";
    /** Token que devuelve el auth simulado y que PostgreSQL guarda en la BD de pruebas. */
    private static final String TOKEN_DESDE_POSTGRES = "Bearer token-mock";

    private static HttpServer server;
    private static int puertoMock;

    /** 0 = responder 200 siempre; 1 = responder 500 siempre. */
    private static final AtomicInteger MODO = new AtomicInteger(0);
    private static final AtomicInteger LLAMADAS_PRODUCTOS = new AtomicInteger(0);
    private static final String[] AUTH_RECIBIDO = new String[1];
    private static final String[] ACCEPT_RECIBIDO = new String[1];

    /** Captura la caché existente justo después del arranque, antes del primer deleteAll. */
    private static Boolean cacheEnArranque;

    @BeforeAll
    static void levantarServicioExternoMock() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        puertoMock = server.getAddress().getPort();
        server.createContext("/sistema/service/getProductList.do", exchange -> {
            LLAMADAS_PRODUCTOS.incrementAndGet();
            AUTH_RECIBIDO[0] = exchange.getRequestHeaders().getFirst("Authorization");
            ACCEPT_RECIBIDO[0] = exchange.getRequestHeaders().getFirst("Accept");
            if (MODO.get() == 1) {
                responder(exchange, 500, "{\"message\":\"error interno\"}");
            } else {
                responderXml(exchange, """
                        <?xml version='1.0' encoding='UTF-8'?><RESPONSE> <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE><PRODUCTOS><producto servicio='Bebidas' producto='Coca Cola 600ml' idServicio='10' idProducto='101' idCatTipoServicio='1' tipoFront='1' hasDigitoVerificador='false' precio='15.50' showAyuda='false' tipoReferencia='a'><legend><![CDATA[Cupon]]></legend></producto><producto servicio='Botanas' producto='Sabritas 45g' idServicio='11' idProducto='102' idCatTipoServicio='1' tipoFront='1' hasDigitoVerificador='false' precio='22.00' showAyuda='false' tipoReferencia='a'><legend><![CDATA[Cupon]]></legend></producto></PRODUCTOS></RESPONSE>
                        """);
            }
        });
        server.createContext("/sistema/app/jwt-gp/authenticate/", exchange ->
                responder(exchange, 200, "{\"token\":\"token-mock\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));
        server.start();
    }

    @AfterAll
    static void apagarServicioExternoMock() {
        if (server != null) {
            server.stop(0);
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

    /** Responde 200 con Content-Type text/xml (formato real del catálogo). */
    private static void responderXml(HttpExchange exchange, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("gestopago.productos.url", () -> "http://localhost:" + puertoMock);
        registry.add("gestopago.productos.token", () -> TOKEN_PRUEBA);
        registry.add("gestopago.productos.reintentos.espera-ms", () -> "10");
        // El refresh de token también se redirige al mock: sin llamadas a internet
        registry.add("gestopago.auth.url", () -> "http://localhost:" + puertoMock);
        // Credenciales aisladas de prueba: no se toca el token real del distribuidor
        registry.add("gestopago.auth.id-distribuidor", () -> "999");
        registry.add("gestopago.auth.codigo-dispositivo", () -> "TEST-CATALOGO-E2E");
    }

    @Autowired
    private ProductoCatalogoService productoCatalogoService;

    @Autowired
    private CatalogoProductosCacheRepository cacheRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @BeforeEach
    void preparar() {
        if (cacheEnArranque == null) {
            cacheEnArranque = cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA).isPresent();
        }
        MODO.set(0);
        LLAMADAS_PRODUCTOS.set(0);
        AUTH_RECIBIDO[0] = null;
        ACCEPT_RECIBIDO[0] = null;
        cacheRepository.deleteAll();
    }

    @Test
    @DisplayName("Al iniciar la aplicación se ejecuta sola la sincronización y llena MongoDB")
    void sincronizaAutomaticamenteAlArrancar() {
        assertThat(cacheEnArranque)
                .as("El ApplicationReadyEvent debe haber guardado el catálogo en la caché")
                .isTrue();
    }

    @Test
    @DisplayName("Almacenar elimina el catálogo anterior y registra solo el nuevo")
    void almacenanEliminaElAnteriorYRegistraElNuevo() {
        // Semilla: catálogo viejo obsoleto con productos que ya no existen
        cacheRepository.save(CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .productos(java.util.List.of(ProductoDTO.builder()
                        .idProducto(999L).producto("SKU-VIEJO-999").build()))
                .build());
        assertThat(cacheRepository.count()).isEqualTo(1);

        productoCatalogoService.sincronizarCatalogo();

        // Solo queda 1 documento y ya no contiene el producto viejo
        assertThat(cacheRepository.count()).isEqualTo(1);
        CatalogoProductosCache cache = cacheRepository
                .findById(CatalogoProductosCache.LLAVE_UNICA).orElseThrow();
        assertThat(cache.getProductos()).hasSize(2);
        assertThat(cache.getProductos())
                .extracting(ProductoDTO::getProducto)
                .containsExactly("Coca Cola 600ml", "Sabritas 45g")
                .doesNotContain("SKU-VIEJO-999");
    }

    @Test
    @DisplayName("Si el servicio externo falla, el catálogo anterior NO se borra")
    void falloExternoConservaElCatalogoAnterior() {
        cacheRepository.save(CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .productos(java.util.List.of(ProductoDTO.builder()
                        .idProducto(555L).producto("SKU-CONSERVADO").build()))
                .build());

        MODO.set(1); // el servicio externo responde 500 siempre
        productoCatalogoService.sincronizarCatalogo();

        CatalogoProductosCache cache = cacheRepository
                .findById(CatalogoProductosCache.LLAVE_UNICA).orElseThrow();
        assertThat(cache.getProductos())
                .extracting(ProductoDTO::getProducto)
                .containsExactly("SKU-CONSERVADO");
    }

    @Test
    @DisplayName("200 a la primera: sincroniza, cachea en MongoDB y el endpoint responde 200 con el JSON")
    void flujoExitosoEndToEnd() {
        productoCatalogoService.sincronizarCatalogo();

        // 1 sola llamada: sin reintentos
        assertThat(LLAMADAS_PRODUCTOS.get()).isEqualTo(1);

        // El Bearer Token vino de PostgreSQL (generado por el sistema al iniciar),
        // no de application.properties (TOKEN_PRUEBA solo queda como respaldo)
        assertThat(AUTH_RECIBIDO[0]).isEqualTo(TOKEN_DESDE_POSTGRES)
                .isNotEqualTo(TOKEN_PRUEBA);

        // Negociación explícita: el servicio externo responde XML
        // (su modo JSON devuelve productos duplicados: bug del origen)
        assertThat(ACCEPT_RECIBIDO[0]).contains("application/xml");

        // Caché en MongoDB
        CatalogoProductosCache cache = cacheRepository
                .findById(CatalogoProductosCache.LLAVE_UNICA).orElseThrow();
        assertThat(cache.getProductos()).hasSize(2);

        // Endpoint local: 200 + JSON del catálogo
        ResponseEntity<String> respuesta =
                testRestTemplate.getForEntity("/catalogo/productos", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody())
                .contains("\"code\":\"200\"")
                .contains("Coca Cola 600ml")
                .contains("Sabritas 45g")
                .contains("\"total\":2");
    }

    @Test
    @DisplayName("Servicio externo con 500: reintenta hasta agotar intentos, emite alerta CRITICAL y no cachea")
    void falloPersistenteDisparaAlertaCritica() {
        MODO.set(1);

        Logger logger = (Logger) LoggerFactory
                .getLogger("com.proyecto.servicios.service.Impl.ProductoCatalogoServiceImpl");
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        LoggerContext contexto = (LoggerContext) logger.getLoggerContext();
        Level nivelOriginal = logger.getLevel();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        try {
            productoCatalogoService.sincronizarCatalogo();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(nivelOriginal);
        }

        // Reintentó 3 veces (intento inicial + 2 reintentos)
        assertThat(LLAMADAS_PRODUCTOS.get()).isEqualTo(3);

        // No guardó caché con datos corruptos
        assertThat(cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA)).isEmpty();

        // Alerta crítica registrada
        boolean alertaCritica = appender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("CRITICAL"));
        assertThat(alertaCritica)
                .as("Se debe registrar una alerta CRITICAL tras agotar los reintentos")
                .isTrue();
    }

    @Test
    @DisplayName("Caché vacía: el endpoint responde 404 con el código del ApiResponseEnum")
    void cacheVaciaResponde404() {
        ResponseEntity<String> respuesta =
                testRestTemplate.getForEntity("/catalogo/productos", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody())
                .contains("\"code\":\"CAT001\"")
                .contains("no se encuentra disponible");
    }

    @Test
    @DisplayName("La sincronización es idempotente: una segunda corrida actualiza el mismo documento")
    void sincronizacionEsIdempotente() {
        productoCatalogoService.sincronizarCatalogo();
        String primeraLlave = cacheRepository
                .findById(CatalogoProductosCache.LLAVE_UNICA).orElseThrow().getId();

        productoCatalogoService.sincronizarCatalogo();

        assertThat(cacheRepository.count()).isEqualTo(1);
        CatalogoProductosCache cache = cacheRepository
                .findById(CatalogoProductosCache.LLAVE_UNICA).orElseThrow();
        assertThat(cache.getId()).isEqualTo(primeraLlave);
        assertThat(cache.getFechaActualizacion()).isNotNull();
        assertThat(cache.getProductos()).hasSize(2);
    }
}
