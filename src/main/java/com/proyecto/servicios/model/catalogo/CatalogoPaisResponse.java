package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada de un país del catálogo.
 * No expone la referencia transitoria a {@code CatalogoEstado}.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoPaisResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(example = "México")
    private String nombre;
}
