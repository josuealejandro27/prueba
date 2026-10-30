package com.proyecto.servicios.client.config;

import com.proyecto.servicios.client.GestoPagoTxFeignInterceptor;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuración exclusiva del cliente OpenFeign de transacciones Gestopago.
 * No lleva {@code @Configuration} para evitar que Spring Boot registre sus interceptores globalmente.
 */
public class GestoPagoTxFeignConfig {

    @Bean
    public RequestInterceptor gestoPagoTxFeignInterceptor(
            GestoPagoTokenService tokenService,
            @Value("${gestopago.tx.token:${gestopago.productos.token:}}") String tokenRespaldo) {
        return new GestoPagoTxFeignInterceptor(tokenService, tokenRespaldo);
    }

    @Bean
    public Request.Options gestoPagoTxRequestOptions(
            @Value("${feign.client.config.gestoPagoTx.connectTimeout:5000}") int connectTimeout,
            @Value("${feign.client.config.gestoPagoTx.readTimeout:15000}") int readTimeout) {
        return new Request.Options(connectTimeout, TimeUnit.MILLISECONDS, readTimeout, TimeUnit.MILLISECONDS, true);
    }
}

