package com.proyecto.servicios.client.config;

import com.proyecto.servicios.client.GestoPagoTxFeignInterceptor;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración exclusiva del cliente OpenFeign de transacciones Gestopago.
 */
@Configuration
public class GestoPagoTxFeignConfig {

    @Bean
    public RequestInterceptor gestoPagoTxFeignInterceptor(
            GestoPagoTokenService tokenService,
            @Value("${gestopago.tx.token:${gestopago.productos.token:}}") String tokenRespaldo) {
        return new GestoPagoTxFeignInterceptor(tokenService, tokenRespaldo);
    }
}

