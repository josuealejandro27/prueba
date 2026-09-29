package com.proyecto.servicios.client;

import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * Inyecta el header Authorization (Bearer Token) en cada petición Feign
 * del catálogo de productos.
 * <p>
 * El token se obtiene de <b>PostgreSQL</b> (tabla {@code gestopago_tokens}),
 * que es donde el sistema lo genera y almacena al iniciar; si no existe o ya
 * expiró, el servicio lo renueva antes de usarlo. Solo si PostgreSQL no
 * devuelve nada se usa, como respaldo, el valor de
 * {@code gestopago.productos.token} en application.properties.
 * <p>
 * Es un bean aislado, registrado únicamente en el alcance del cliente Feign
 * mediante {@link com.proyecto.servicios.client.config.CatalogoFeignConfig}.
 */
@Slf4j
public class CatalogoTokenFeignInterceptor implements RequestInterceptor {

    private final GestoPagoTokenService tokenService;
    private final String tokenRespaldo;

    public CatalogoTokenFeignInterceptor(GestoPagoTokenService tokenService, String tokenRespaldo) {
        this.tokenService = tokenService;
        this.tokenRespaldo = tokenRespaldo;
    }

    @Override
    public void apply(RequestTemplate template) {
        String authorization = tokenService.obtenerTokenVigente()
                .map(this::comoBearer)
                .orElseGet(() -> {
                    log.warn("Sin token vigente en PostgreSQL para el catálogo; "
                            + "se usa el respaldo de application.properties");
                    return tokenRespaldo;
                });

        if (authorization != null && !authorization.isBlank()) {
            // Se limpia antes para evitar headers duplicados
            template.headers().remove("Authorization");
            template.header("Authorization", authorization.trim());
        }

        // El servicio externo responde XML (el modo JSON devuelve datos
        // duplicados: bug del origen); se negocia application/xml explícito.
        template.headers().remove("Accept");
        template.header("Accept", "application/xml");
    }

    /** Agrega el prefijo Bearer solo si el token almacenado no lo trae ya. */
    private String comoBearer(String token) {
        String limpio = token.trim();
        return limpio.toUpperCase(Locale.ROOT).startsWith("BEARER") ? limpio : "Bearer " + limpio;
    }
}
