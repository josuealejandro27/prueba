package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada de un estado civil del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoCivilResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Soltero")
    private String nombre;
}
