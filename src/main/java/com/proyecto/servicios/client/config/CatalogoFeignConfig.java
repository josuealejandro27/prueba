package com.proyecto.servicios.client.config;

import com.proyecto.servicios.client.CatalogoTokenFeignInterceptor;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración exclusiva del cliente Feign del catálogo de productos.
 * Al ser declarada en {@code @FeignClient(configuration = ...)},
 * sus beans solo aplican a ese cliente (no a los demás Feign del sistema).
 * <p>
 * El Bearer Token se resuelve por petición desde PostgreSQL (donde el
 * sistema lo genera al iniciar); el valor de properties es solo respaldo.
 */
@Configuration
public class CatalogoFeignConfig {

    @Bean
    public RequestInterceptor catalogoTokenFeignInterceptor(
            GestoPagoTokenService tokenService,
            @Value("${gestopago.productos.token:}") String tokenRespaldo) {
        return new CatalogoTokenFeignInterceptor(tokenService, tokenRespaldo);
    }
}
