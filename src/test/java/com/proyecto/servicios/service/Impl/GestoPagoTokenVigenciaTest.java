package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Token vigente en PostgreSQL para el catálogo")
class GestoPagoTokenVigenciaTest {

    @Mock
    private GestoPagoAuthClient authClient;

    @Mock
    private GestoPagoTokenRepository repository;

    @Mock
    private GestoPagoTokenMapper mapper;

    private GestoPagoTokenServiceImpl service;

    @BeforeEach
    void configurar() {
        service = new GestoPagoTokenServiceImpl(authClient, repository, mapper);
    }

    private GestoPagoToken tokenValido(String valor) {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken(valor);
        token.setActivo(true);
        token.setExpiresIn(3600L);
        token.setFechaActualizacion(LocalDateTime.now());
        return token;
    }

    private GestoPagoToken tokenExpirado() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-viejo");
        token.setActivo(true);
        token.setExpiresIn(3600L);
        token.setFechaActualizacion(LocalDateTime.now().minusHours(2));
        return token;
    }

    private GestoPagoAuthResponse respuestaAuth(String valor) {
        GestoPagoAuthResponse respuesta = new GestoPagoAuthResponse();
        respuesta.setToken(valor);
        respuesta.setTokenType("Bearer");
        respuesta.setExpiresIn(3600L);
        return respuesta;
    }

    @Test
    @DisplayName("Token vigente: lo devuelve de PostgreSQL sin llamar al servicio de autenticación")
    void tokenVigenteSinRenovar() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(any(), any()))
                .thenReturn(Optional.of(tokenValido("abc123")));

        assertThat(service.obtenerTokenVigente()).contains("abc123");

        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("Token expirado: lo renueva automáticamente y devuelve el nuevo")
    void tokenExpiradoSeRenueva() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(any(), any()))
                .thenReturn(Optional.of(tokenExpirado()))                 // consulta 1: expirado
                .thenReturn(Optional.of(tokenExpirado()))                 // consulta 2: dentro de renovar
                .thenReturn(Optional.of(tokenValido("nuevo-abc")));       // consulta 3: tras renovar
        when(authClient.authenticate(any(), any(), any()))
                .thenReturn(respuestaAuth("nuevo-abc"));

        assertThat(service.obtenerTokenVigente()).contains("nuevo-abc");

        verify(authClient, times(1)).authenticate(any(), any(), any());
        verify(repository, atLeastOnce()).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("Sin token en la BD: lo genera y lo almacena")
    void sinTokenLoGenera() {
        GestoPagoToken creado = tokenValido("recien-creado");
        when(repository.findByIdDistribuidorAndCodigoDispositivo(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(creado));
        when(authClient.authenticate(any(), any(), any()))
                .thenReturn(respuestaAuth("recien-creado"));
        when(mapper.toEntity(any(GestoPagoAuthResponse.class))).thenReturn(creado);

        assertThat(service.obtenerTokenVigente()).contains("recien-creado");

        verify(repository).save(creado);
    }

    @Test
    @DisplayName("Si la renovación falla devuelve vacío sin lanzar excepción")
    void renovacionFallidaDevuelveVacio() {
        when(repository.findByIdDistribuidorAndCodigoDispositivo(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(authClient.authenticate(any(), any(), any()))
                .thenThrow(new RuntimeException("servidor de autenticación caído"));

        assertThat(service.obtenerTokenVigente()).isEmpty();
    }
}
