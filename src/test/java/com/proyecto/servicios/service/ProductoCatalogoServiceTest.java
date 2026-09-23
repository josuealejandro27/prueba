package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoCatProduct;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoCacheResponseDTO;
import com.proyecto.servicios.model.catalogo.CatalogoProductosResponse;
import com.proyecto.servicios.model.catalogo.MensajeDTO;
import com.proyecto.servicios.model.catalogo.ProductoDTO;
import com.proyecto.servicios.repositorys.mongo.CatalogoProductosCacheRepository;
import com.proyecto.servicios.service.Impl.ProductoCatalogoServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoCatalogoServiceTest {

    @Mock
    private GestoPagoCatProduct gestoPagoCatProduct;

    @Mock
    private CatalogoProductosCacheRepository cacheRepository;

    private ProductoCatalogoService service;

    @BeforeEach
    void setUp() {
        // 3 intentos, espera mínima para que el test sea instantáneo
        service = new ProductoCatalogoServiceImpl(gestoPagoCatProduct, cacheRepository, 3, 1L);
    }

    private CatalogoProductosResponse respuestaOk() {
        ProductoDTO producto = ProductoDTO.builder()
                .idProducto(1L)
                .producto("Producto de prueba")
                .precio(new java.math.BigDecimal("10.50"))
                .build();
        return CatalogoProductosResponse.builder()
                .mensaje(MensajeDTO.builder().codigo("01").texto("Operacion realizada con exito").build())
                .productos(List.of(producto))
                .build();
    }

    private FeignException errorHttp(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8, new RequestTemplate());
        feign.Response response = feign.Response.builder()
                .request(request)
                .status(status)
                .reason("reason-" + status)
                .headers(Collections.emptyMap())
                .body("", java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("HTTP " + status, response);
    }

    @Test
    @DisplayName("200 a la primera: no debe reintentar y debe reemplazar la caché")
    void exitoALaPrimeraNoReintenta() {
        when(gestoPagoCatProduct.obtenerListaProductos()).thenReturn(respuestaOk());
        when(cacheRepository.save(any(CatalogoProductosCache.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.sincronizarCatalogo();

        verify(gestoPagoCatProduct, times(1)).obtenerListaProductos();
        // Reemplazo: primero borra el anterior, luego registra el nuevo
        var orden = inOrder(cacheRepository);
        orden.verify(cacheRepository).deleteAll();
        orden.verify(cacheRepository).save(any(CatalogoProductosCache.class));
    }

    @Test
    @DisplayName("El arranque de la aplicación ejecuta la misma sincronización completa")
    void arranqueEjecutaSincronizacion() {
        when(gestoPagoCatProduct.obtenerListaProductos()).thenReturn(respuestaOk());
        when(cacheRepository.save(any(CatalogoProductosCache.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.sincronizarCatalogoAlArranque();

        verify(gestoPagoCatProduct, times(1)).obtenerListaProductos();
        var orden = inOrder(cacheRepository);
        orden.verify(cacheRepository).deleteAll();
        orden.verify(cacheRepository).save(any(CatalogoProductosCache.class));
    }

    @Test
    @DisplayName("Error HTTP transitorio: reintenta hasta tener éxito")
    void reintentaHastaExitos() {
        when(gestoPagoCatProduct.obtenerListaProductos())
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(respuestaOk());
        when(cacheRepository.save(any(CatalogoProductosCache.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.sincronizarCatalogo();

        verify(gestoPagoCatProduct, times(3)).obtenerListaProductos();
        verify(cacheRepository, times(1)).deleteAll();
        verify(cacheRepository, times(1)).save(any(CatalogoProductosCache.class));
    }

    @Test
    @DisplayName("Sin 200 no se borra ni se escribe: el catálogo anterior se conserva")
    void agotaReintentosNoGuardaCache() {
        when(gestoPagoCatProduct.obtenerListaProductos()).thenThrow(errorHttp(500));

        // No debe lanzar: la alerta se emite como log CRITICAL
        service.sincronizarCatalogo();

        verify(gestoPagoCatProduct, times(3)).obtenerListaProductos();
        verify(cacheRepository, never()).deleteAll();
        verify(cacheRepository, never()).save(any(CatalogoProductosCache.class));
    }

    @Test
    @DisplayName("Fallo de autenticación (401) también se reintenta y no toca la caché")
    void errorAutenticacionSeReintenta() {
        when(gestoPagoCatProduct.obtenerListaProductos()).thenThrow(errorHttp(401));

        // No debe lanzar: la alerta crítica de autenticación se emite como log
        service.sincronizarCatalogo();

        verify(gestoPagoCatProduct, times(3)).obtenerListaProductos();
        verify(cacheRepository, never()).deleteAll();
        verify(cacheRepository, never()).save(any(CatalogoProductosCache.class));
    }

    @Test
    @DisplayName("Consulta a caché vacía lanza CatalogoException 404")
    void cacheVaciaLanzaExcepcion() {
        when(cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerCatalogoCacheado())
                .isInstanceOf(CatalogoException.class)
                .extracting(ex -> ((CatalogoException) ex).getResponse())
                .isEqualTo(ApiResponseEnum.CATALOGO_NO_DISPONIBLE);
    }

    @Test
    @DisplayName("Consulta a caché disponible devuelve 200 con el JSON del catálogo")
    void cacheDisponibleDevuelveCatalogo() {
        CatalogoProductosCache cache = CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .productos(List.of(ProductoDTO.builder().idProducto(9L).producto("Prod 9").build()))
                .build();
        when(cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA)).thenReturn(Optional.of(cache));

        CatalogoCacheResponseDTO resultado = service.obtenerCatalogoCacheado();

        assertThat(resultado.getTotal()).isEqualTo(1);
        assertThat(resultado.getProductos()).hasSize(1);
        assertThat(resultado.getProductos().get(0).getProducto()).isEqualTo("Prod 9");
    }

    @Test
    @DisplayName("La tarea programada usa el cron de las 06:00 AM")
    void cronEsSeisDeLaManana() throws NoSuchMethodException {
        Scheduled anotacion = ProductoCatalogoServiceImpl.class
                .getMethod("sincronizarCatalogo")
                .getAnnotation(Scheduled.class);

        assertThat(anotacion).as("El método debe tener @Scheduled").isNotNull();
        // La anotación usa placeholder con default: el default debe ser 0 0 6 * * *
        assertThat(anotacion.cron()).contains("0 0 6 * * *");
    }

    @Test
    @DisplayName("El reemplazo en caché borra primero y registra después, con llave única")
    void guardadoUsaLlaveUnica() {
        when(gestoPagoCatProduct.obtenerListaProductos()).thenReturn(respuestaOk());
        when(cacheRepository.save(any(CatalogoProductosCache.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.sincronizarCatalogo();

        ArgumentCaptor<CatalogoProductosCache> captor = ArgumentCaptor.forClass(CatalogoProductosCache.class);
        var orden = inOrder(cacheRepository);
        orden.verify(cacheRepository).deleteAll();
        orden.verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(CatalogoProductosCache.LLAVE_UNICA);
        assertThat(captor.getValue().getProductos()).hasSize(1);
        assertThat(captor.getValue().getFechaActualizacion()).isNotNull();
    }
}
