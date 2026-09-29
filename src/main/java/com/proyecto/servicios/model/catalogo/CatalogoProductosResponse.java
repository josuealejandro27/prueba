package com.proyecto.servicios.model.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta cruda que devuelve el servicio externo
 * GET /sistema/service/getProductList.do (XML).
 *
 * <pre>
 * &lt;RESPONSE&gt;
 *   &lt;MENSAJE&gt;&lt;CODIGO&gt;01&lt;/CODIGO&gt;&lt;TEXTO&gt;Operacion realizada con exito&lt;/TEXTO&gt;/MENSAJE&gt;
 *   &lt;PRODUCTOS&gt;&lt;producto idProducto='14302' producto='ABIB 100' .../&gt;...&lt;/PRODUCTOS&gt;
 * &lt;/RESPONSE&gt;
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class CatalogoProductosResponse {

    @JsonProperty("mensaje")
    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeDTO mensaje;

    @JsonProperty("productos")
    @JacksonXmlElementWrapper(localName = "PRODUCTOS")
    @JacksonXmlProperty(localName = "producto")
    private List<ProductoDTO> productos;

    /**
     * Normaliza el listado para que nunca sea null.
     *
     * @return lista de productos (posiblemente vacía).
     */
    public List<ProductoDTO> getProductos() {
        return productos == null ? List.of() : productos;
    }
}
