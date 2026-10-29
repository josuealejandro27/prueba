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
@DisplayName("Interceptor OpenFeign para sendTx.do")
class GestoPagoTxFeignInterceptorTest {

    private static final String RESPALDO = "Bearer RESPALDO_TX_PROPS";

    @Mock
    private GestoPagoTokenService tokenService;

    private GestoPagoTxFeignInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new GestoPagoTxFeignInterceptor(tokenService, RESPALDO);
    }

    @Test
    @DisplayName("Inyecta token Bearer de PostgreSQL y encabezados Content-Type y Accept JSON")
    void inyectaTokenYEncabezados() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.of("token-vigente-123"));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).containsKey("Authorization");
        assertThat(template.headers().get("Authorization")).containsExactly("Bearer token-vigente-123");

        assertThat(template.headers()).containsKey("Content-Type");
        assertThat(template.headers().get("Content-Type")).containsExactly("application/json; charset=UTF-8");

        assertThat(template.headers()).containsKey("Accept");
        assertThat(template.headers().get("Accept")).containsExactly("application/json, application/xml");
    }

    @Test
    @DisplayName("No duplica el prefijo si el token ya incluye 'Bearer'")
    void noDuplicaPrefijoBearer() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.of("Bearer mi-token-ya-formateado"));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer mi-token-ya-formateado");
    }

    @Test
    @DisplayName("Si PostgreSQL no tiene token, usa el token de respaldo")
    void usaTokenRespaldoSiBdVacia() {
        when(tokenService.obtenerTokenVigente()).thenReturn(Optional.empty());

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer RESPALDO_TX_PROPS");
    }
}

