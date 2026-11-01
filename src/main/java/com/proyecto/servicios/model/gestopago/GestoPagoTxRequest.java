package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO que modela el payload enviado al endpoint sendTx.do de Gestopago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoTxRequest {

    @JsonProperty("idProveedor")
    private Integer idProveedor;

    @JsonProperty("idService")
    private Long idService;

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("horaLocal")
    private String horaLocal;

    @JsonProperty("montoPago")
    private BigDecimal montoPago;

    @JsonProperty("upc")
    private String upc;

    @JsonProperty("unidad")
    @Builder.Default
    private String unidad = "1";

    @JsonProperty("firma")
    private String firma;
}

