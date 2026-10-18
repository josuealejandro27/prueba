package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada del catálogo de estados / entidades federativas.
 * Expone el país mediante su identificador y nombre (nunca la entidad JPA).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "14")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Morelos")
    private String nombre;

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long paisId;

    @io.swagger.v3.oas.annotations.media.Schema(example = "México")
    private String paisNombre;
}
