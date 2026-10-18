package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada del catálogo de municipios.
 * Expone el estado mediante su identificador y nombre (nunca la entidad JPA).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoMunicipioResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "37")
    private Long id;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Cuernavaca")
    private String nombre;

    @io.swagger.v3.oas.annotations.media.Schema(example = "14")
    private Long estadoId;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Morelos")
    private String estadoNombre;
}
