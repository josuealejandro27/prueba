package com.proyecto.servicios.client;

import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bearer Token del catálogo resuelto desde PostgreSQL")
class CatalogoTokenFeignInterceptorTest {

    private static final String RESPALDO = "Bearer RESPALDO_PROPS";

    @Mock
    private GestoPagoTokenService tokenService;

    private CatalogoTokenFeignInterceptor interceptor;

    @BeforeEach
    void configurar() {
        interceptor = new CatalogoTokenFeignInterceptor(tokenService, RESPALDO);
    }

    @Test
    @DisplayName("Usa el token de PostgreSQL con prefijo Bearer")
    void usaTokenDePostgres() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.of("abc123"));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).containsKey("Authorization");
        assertThat(template.headers().get("Authorization")).containsExactly("Bearer abc123");
    }

    @Test
    @DisplayName("Negocia application/xml: el modo JSON del servicio devuelve datos duplicados")
    void negociaXml() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.of("abc123"));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Accept")).containsExactly("application/xml");
    }

    @Test
    @DisplayName("No duplica el prefijo si el token almacenado ya viene con Bearer")
    void noDuplicaPrefijo() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.of("Bearer ya-venido"));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer ya-venido");
    }

    @Test
    @DisplayName("Si PostgreSQL no tiene token, usa el respaldo de application.properties")
    void respaldoDeProperties() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.empty());

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly(RESPALDO);
    }

    @Test
    @DisplayName("Sin token en la BD ni respaldo configurado: no se envía la cabecera")
    void sinCabeceraSiNoHayNada() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.empty());
        interceptor = new CatalogoTokenFeignInterceptor(tokenService, "");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}
