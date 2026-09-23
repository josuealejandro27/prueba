package com.proyecto.servicios.model.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta del endpoint local que expone el catálogo
 * consultado desde la caché de MongoDB.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogoCacheResponseDTO {

    private Integer total;

    private LocalDateTime fechaActualizacion;

    private List<ProductoDTO> productos;
}
