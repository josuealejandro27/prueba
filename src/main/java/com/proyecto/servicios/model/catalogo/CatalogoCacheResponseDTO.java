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

    @io.swagger.v3.oas.annotations.media.Schema(example = "901")
    private Integer total;

    @io.swagger.v3.oas.annotations.media.Schema(example = "2026-01-15T10:30:00")
    private LocalDateTime fechaActualizacion;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Productos del catálogo sincronizado")
    private List<ProductoDTO> productos;
}
