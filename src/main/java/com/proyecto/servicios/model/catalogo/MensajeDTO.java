package com.proyecto.servicios.model.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bloque "mensaje" de la respuesta del servicio externo:
 * código de negocio y texto de la operación (XML: {@code <MENSAJE>}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MensajeDTO {

    /** Código de negocio (ej. "01" = operación realizada con éxito). */
    @JsonProperty("codigo")
    @JacksonXmlProperty(localName = "CODIGO")
    private String codigo;

    @JsonProperty("texto")
    @JacksonXmlProperty(localName = "TEXTO")
    private String texto;
}
