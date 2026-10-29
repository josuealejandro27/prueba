package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas de mapeo y comportamiento de DTOs del cliente Gestopago Tx")
class GestoPagoTxClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deserializar respuesta estándar con código 0 (éxito)")
    void deserializarRespuestaCodigo0() throws Exception {
        String json = "{\"codigo\":\"0\",\"mensaje\":\"TRANSACCION EXITOSA\",\"folio\":\"AUT-12345\",\"fecha\":\"2026-10-28 22:00:00\",\"status\":200}";

        GestoPagoTxResponse response = objectMapper.readValue(json, GestoPagoTxResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isEqualTo("0");
        assertThat(response.getMensaje()).isEqualTo("TRANSACCION EXITOSA");
        assertThat(response.getFolio()).isEqualTo("AUT-12345");
        assertThat(response.esExitosa()).isTrue();
    }

    @Test
    @DisplayName("Deserializar respuesta con código 01 (éxito según especificación)")
    void deserializarRespuestaCodigo01() throws Exception {
        String json = "{\"codigo\":\"01\",\"mensaje\":\"APROBADA\",\"folio\":\"AUT-99999\"}";

        GestoPagoTxResponse response = objectMapper.readValue(json, GestoPagoTxResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isEqualTo("01");
        assertThat(response.esExitosa()).isTrue();
    }

    @Test
    @DisplayName("Deserializar respuesta de error o rechazo (esExitosa = false)")
    void deserializarRespuestaRechazo() throws Exception {
        String json = "{\"codigo\":\"ERR-99\",\"mensaje\":\"REFERENCIA INVALIDA O INEXISTENTE\"}";

        GestoPagoTxResponse response = objectMapper.readValue(json, GestoPagoTxResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isEqualTo("ERR-99");
        assertThat(response.esExitosa()).isFalse();
    }

    @Test
    @DisplayName("Tolerancia a nombres alternativos (aliases) de campos en la respuesta")
    void toleranciaAAliasesEnRespuesta() throws Exception {
        String json = "{\"code\":\"0\",\"message\":\"PAGO APLICADO\",\"ticket\":\"TK-554433\"}";

        GestoPagoTxResponse response = objectMapper.readValue(json, GestoPagoTxResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isEqualTo("0");
        assertThat(response.getMensaje()).isEqualTo("PAGO APLICADO");
        assertThat(response.getFolio()).isEqualTo("TK-554433");
        assertThat(response.esExitosa()).isTrue();
    }

    @Test
    @DisplayName("Serializar GestoPagoTxRequest a JSON para la petición")
    void serializarRequestTx() throws Exception {
        GestoPagoTxRequest request = GestoPagoTxRequest.builder()
                .idProveedor(83)
                .idService(101L)
                .telefono("5512345678")
                .horaLocal("2026-10-28 22:00:00")
                .montoPago(new BigDecimal("99.90"))
                .upc("123456789012")
                .unidad("1")
                .firma("firma-cifrada-base64")
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"idProveedor\":83");
        assertThat(json).contains("\"idService\":101");
        assertThat(json).contains("\"telefono\":\"5512345678\"");
        assertThat(json).contains("\"montoPago\":99.90");
        assertThat(json).contains("\"firma\":\"firma-cifrada-base64\"");
    }
}

