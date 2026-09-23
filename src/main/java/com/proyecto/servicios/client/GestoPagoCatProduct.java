package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.CatalogoFeignConfig;
import com.proyecto.servicios.model.catalogo.CatalogoProductosResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cliente OpenFeign del catálogo de productos.
 * <p>
 * El header Authorization (Bearer Token) lo inyecta
 * {@link CatalogoTokenFeignInterceptor}, configurado en
 * {@link com.proyecto.servicios.client.config.CatalogoFeignConfig}: el token se
 * resuelve desde PostgreSQL (donde el sistema lo genera al iniciar) y se envía
 * además {@code Accept: application/xml}, formato real del servicio (su modo
 * JSON devuelve productos duplicados).
 */
@FeignClient(
        name = "gestoPagoCatalogo",
        url = "${gestopago.productos.url}",
        configuration = CatalogoFeignConfig.class
)
public interface GestoPagoCatProduct {

    @GetMapping("${gestopago.productos.ruta}")
    CatalogoProductosResponse obtenerListaProductos();
}
