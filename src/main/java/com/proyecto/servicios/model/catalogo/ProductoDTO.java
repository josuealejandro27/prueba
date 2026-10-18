package com.proyecto.servicios.model.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de transferencia que representa un producto del catálogo externo
 * (GET /sistema/service/getProductList.do, respuesta XML real de GestoPago).
 * <p>
 * En el XML del origen los datos van como <b>atributos</b> de
 * {@code <producto ...>} y {@code legend} como elemento hijo; las anotaciones
 * {@code @JsonProperty} conservan el nombre de campo al exponer el catálogo
 * desde nuestro endpoint JSON. Ignora campos desconocidos para tolerar
 * cambios en el servicio externo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoDTO {

    @JsonProperty("idproducto")
    @JacksonXmlProperty(isAttribute = true, localName = "idProducto")
    @io.swagger.v3.oas.annotations.media.Schema(example = "101")
    private Long idProducto;

    /** Nombre comercial del producto (ej. "ABIB 100"). */
    @JsonProperty("producto")
    @JacksonXmlProperty(isAttribute = true, localName = "producto")
    @io.swagger.v3.oas.annotations.media.Schema(example = "ABIB 100")
    private String producto;

    /** Servicio o categoría a la que pertenece (ej. "ABIB"). */
    @JsonProperty("servicio")
    @JacksonXmlProperty(isAttribute = true, localName = "servicio")
    @io.swagger.v3.oas.annotations.media.Schema(example = "ABIB")
    private String servicio;

    @JsonProperty("precio")
    @JacksonXmlProperty(isAttribute = true, localName = "precio")
    @io.swagger.v3.oas.annotations.media.Schema(example = "99.90")
    private BigDecimal precio;

    @JsonProperty("idservicio")
    @JacksonXmlProperty(isAttribute = true, localName = "idServicio")
    private Long idServicio;

    @JsonProperty("idcattiposervicio")
    @JacksonXmlProperty(isAttribute = true, localName = "idCatTipoServicio")
    private Long idCatTipoServicio;

    @JsonProperty("tipofront")
    @JacksonXmlProperty(isAttribute = true, localName = "tipoFront")
    private Integer tipoFront;

    @JsonProperty("tiporeferencia")
    @JacksonXmlProperty(isAttribute = true, localName = "tipoReferencia")
    private String tipoReferencia;

    /** Texto legal/instrucciones del cupón: elemento hijo {@code <legend>}. */
    @JsonProperty("legend")
    private String legend;

    @JsonProperty("hasdigitoverificador")
    @JacksonXmlProperty(isAttribute = true, localName = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @JsonProperty("showayuda")
    @JacksonXmlProperty(isAttribute = true, localName = "showAyuda")
    private Boolean showAyuda;
}
