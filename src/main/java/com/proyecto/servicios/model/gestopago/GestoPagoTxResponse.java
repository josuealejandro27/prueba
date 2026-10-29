package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que modela la respuesta del endpoint sendTx.do de Gestopago.
 * Tolerante a variaciones de formato y propiedades desconocidas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoTxResponse {

    /**
     * Código de respuesta del proveedor. "0" o "01" indican éxito según especificación.
     */
    @JsonProperty("codigo")
    @JsonAlias({"code", "codigoRespuesta", "codRespuesta"})
    private String codigo;

    @JsonProperty("mensaje")
    @JsonAlias({"message", "mensajeRespuesta", "msg"})
    private String mensaje;

    @JsonProperty("folio")
    @JsonAlias({"ticket", "folioTransaccion", "folioOperacion", "idTx"})
    private String folio;

    @JsonProperty("fecha")
    @JsonAlias({"fechaHora", "timestamp"})
    private String fecha;

    @JsonProperty("status")
    private Integer status;

    /**
     * Determina si la respuesta del proveedor representa una transacción exitosa/aprobada.
     */
    public boolean esExitosa() {
        if (codigo == null) {
            return false;
        }
        String limpio = codigo.trim();
        return "0".equals(limpio) || "00".equals(limpio) || "01".equals(limpio) || "1".equals(limpio);
    }
}

