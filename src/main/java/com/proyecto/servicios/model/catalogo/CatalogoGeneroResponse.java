package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada de un género del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoGeneroResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Masculino")
    private String tipo;
}
