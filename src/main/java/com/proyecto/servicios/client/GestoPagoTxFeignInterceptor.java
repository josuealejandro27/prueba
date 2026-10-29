package com.proyecto.servicios.client;

import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * Interceptor OpenFeign que inyecta el token Bearer vigente y los encabezados requeridos
 * para el endpoint de transacciones sendTx.do de Gestopago.
 */
@Slf4j
public class GestoPagoTxFeignInterceptor implements RequestInterceptor {

    private final GestoPagoTokenService tokenService;
    private final String tokenRespaldo;

    public GestoPagoTxFeignInterceptor(GestoPagoTokenService tokenService, String tokenRespaldo) {
        this.tokenService = tokenService;
        this.tokenRespaldo = tokenRespaldo;
    }

    @Override
    public void apply(RequestTemplate template) {
        String authorization = tokenService.obtenerTokenVigente()
                .map(this::comoBearer)
                .orElseGet(() -> {
                    log.warn("Sin token vigente en PostgreSQL para sendTx; usando respaldo de configuración");
                    return tokenRespaldo != null && !tokenRespaldo.isBlank() ? comoBearer(tokenRespaldo) : null;
                });

        if (authorization != null && !authorization.isBlank()) {
            template.headers().remove("Authorization");
            template.header("Authorization", authorization.trim());
        }

        template.headers().remove("Content-Type");
        template.header("Content-Type", "application/json; charset=UTF-8");

        template.headers().remove("Accept");
        template.header("Accept", "application/json, application/xml");
    }

    private String comoBearer(String token) {
        String limpio = token.trim();
        return limpio.toUpperCase(Locale.ROOT).startsWith("BEARER") ? limpio : "Bearer " + limpio;
    }
}

