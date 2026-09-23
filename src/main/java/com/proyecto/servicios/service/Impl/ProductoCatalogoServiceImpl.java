package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoCatProduct;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoCacheResponseDTO;
import com.proyecto.servicios.model.catalogo.CatalogoProductosResponse;
import com.proyecto.servicios.model.catalogo.MensajeDTO;
import com.proyecto.servicios.model.catalogo.ProductoDTO;
import com.proyecto.servicios.repositorys.mongo.CatalogoProductosCacheRepository;
import com.proyecto.servicios.service.ProductoCatalogoService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación del catálogo de productos.
 * <ul>
 *   <li>Tarea programada diaria 06:00 AM (cron configurable).</li>
 *   <li>Consumo del servicio externo vía OpenFeign con Bearer Token.</li>
 *   <li>Política de reintentos y alerta crítica si todos fallan.</li>
 *   <li>Caché exclusiva del snapshot en MongoDB.</li>
 * </ul>
 */
@Service
@Slf4j
public class ProductoCatalogoServiceImpl implements ProductoCatalogoService {

    /** Código de negocio que GestoPago devuelve cuando la operación fue exitosa. */
    private static final String CODIGO_EXITO = "01";

    private final GestoPagoCatProduct gestoPagoCatProduct;
    private final CatalogoProductosCacheRepository cacheRepository;
    private final RetryTemplate retryTemplate;

    public ProductoCatalogoServiceImpl(GestoPagoCatProduct gestoPagoCatProduct,
                                       CatalogoProductosCacheRepository cacheRepository,
                                       @Value("${gestopago.productos.reintentos.max-intentos:3}") int maxIntentos,
                                       @Value("${gestopago.productos.reintentos.espera-ms:2000}") long esperaMs) {
        this.gestoPagoCatProduct = gestoPagoCatProduct;
        this.cacheRepository = cacheRepository;
        this.retryTemplate = construirRetryTemplate(maxIntentos, esperaMs);
    }

    /**
     * Construye la política de reintentos: reintenta ante cualquier error
     * de comunicación (timeout, 4xx/5xx, error de decodificación) con una
     * espera fija entre intentos.
     */
    private RetryTemplate construirRetryTemplate(int maxIntentos, long esperaMs) {
        SimpleRetryPolicy politica = new SimpleRetryPolicy(maxIntentos);

        FixedBackOffPolicy backoff = new FixedBackOffPolicy();
        backoff.setBackOffPeriod(esperaMs);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(politica);
        template.setBackOffPolicy(backoff);
        template.setListeners(new RetryListener[]{intentoLogger()});
        return template;
    }

    /**
     * Deja traza de cada reintento para diagnóstico operativo.
     */
    private RetryListener intentoLogger() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                return true;
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                                                       Throwable throwable) {
                if (throwable != null) {
                    log.error("Catálogo de productos: se agotaron los reintentos tras {} intento(s)",
                            context.getRetryCount());
                }
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                         Throwable throwable) {
                log.warn("Catálogo de productos: intento {} fallido -> {}",
                        context.getRetryCount(), throwable.getMessage());
            }
        };
    }

    @Override
    @Scheduled(cron = "${gestopago.productos.cron:0 0 6 * * *}")
    public void sincronizarCatalogo() {
        log.info("Iniciando sincronización programada del catálogo de productos");
        try {
            CatalogoProductosResponse respuesta = retryTemplate.execute(
                    (RetryCallback<CatalogoProductosResponse, Exception>) context ->
                            invocarServicioExterno());

            // Solo se llega aquí si el servicio externo respondió 200
            reemplazarEnCache(respuesta);
            log.info("Catálogo de productos sincronizado correctamente en MongoDB ({} productos)",
                    respuesta.getProductos().size());

        } catch (Exception e) {
            // Alerta: tras agotar los reintentos el servicio sigue fallando
            if (esFalloDeAutenticacion(e)) {
                log.error("CRITICAL - ALERTA CATÁLOGO PRODUCTOS: la autenticación fue rechazada (HTTP 401/403) "
                        + "tras agotar los reintentos. Revisar el Bearer Token configurado. Causa: {}",
                        e.getMessage(), e);
            } else {
                log.error("CRITICAL - ALERTA CATÁLOGO PRODUCTOS: servicio externo no disponible tras reintentos. "
                        + "Causa: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Determina si la causa raíz del fallo fue un rechazo de autenticación,
     * para clasificar la alerta crítica.
     */
    private boolean esFalloDeAutenticacion(Throwable e) {
        Throwable actual = e;
        while (actual != null) {
            if (actual instanceof FeignException.Unauthorized || actual instanceof FeignException.Forbidden) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    /**
     * Invoca el servicio externo y valida que la respuesta sea considerada
     * exitosa. Cualquier fallo (timeout, 4xx/5xx, respuesta vacía) se propaga
     * para que {@link RetryTemplate} ejecute el reintento automático.
     */
    private CatalogoProductosResponse invocarServicioExterno() {
        CatalogoProductosResponse respuesta = gestoPagoCatProduct.obtenerListaProductos();

        if (Objects.isNull(respuesta)) {
            throw new CatalogoException(ApiResponseEnum.ERROR_COMUNICACION,
                    "El servicio externo devolvió una respuesta vacía");
        }
        return respuesta;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarCatalogoAlArranque() {
        log.info("Aplicación iniciada: ejecutando sincronización inicial del catálogo de productos");
        sincronizarCatalogo();
    }

    /**
     * Reemplaza por completo el catálogo cacheado en MongoDB: elimina el
     * snapshot anterior y registra el nuevo.
     * <p>
     * Solo se invoca cuando el servicio externo respondió HTTP 200; si la
     * petición falla, este método no se ejecuta y el catálogo anterior se
     * conserva intacto.
     *
     * @param respuesta respuesta exitosa (200) del servicio externo.
     */
    private void reemplazarEnCache(CatalogoProductosResponse respuesta) {
        List<ProductoDTO> productos = respuesta.getProductos();
        MensajeDTO mensaje = respuesta.getMensaje();

        if (mensaje != null && !CODIGO_EXITO.equals(mensaje.getCodigo())) {
            log.warn("El servicio externo respondió HTTP 200 pero con código de negocio '{}' ({}); "
                    + "se cachea el catálogo igual (regla: solo se guarda con 200)",
                    mensaje.getCodigo(), mensaje.getTexto());
        }

        log.info("Reemplazando catálogo en caché: eliminando el anterior y registrando {} productos",
                productos.size());

        cacheRepository.deleteAll();

        CatalogoProductosCache cache = CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .fechaActualizacion(LocalDateTime.now())
                .estatusOrigen(mensaje == null ? null : mensaje.getCodigo())
                .mensajeOrigen(mensaje == null ? null : mensaje.getTexto())
                .productos(productos)
                .build();

        cacheRepository.save(cache);
    }

    @Override
    public CatalogoCacheResponseDTO obtenerCatalogoCacheado() {
        Optional<CatalogoProductosCache> cache =
                cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA);

        if (cache.isEmpty() || cache.get().getProductos() == null) {
            throw new CatalogoException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE);
        }

        CatalogoProductosCache catalogo = cache.get();
        return CatalogoCacheResponseDTO.builder()
                .total(catalogo.getProductos().size())
                .fechaActualizacion(catalogo.getFechaActualizacion())
                .productos(catalogo.getProductos())
                .build();
    }
}
