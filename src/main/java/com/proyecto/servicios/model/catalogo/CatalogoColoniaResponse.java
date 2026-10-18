package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada del catálogo de colonias.
 * Expone el municipio mediante su identificador y nombre (nunca la entidad JPA).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoColoniaResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "101")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Centro")
    private String nombre;

    @io.swagger.v3.oas.annotations.media.Schema(example = "37")
    private Long municipioId;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Cuernavaca")
    private String municipioNombre;
}
