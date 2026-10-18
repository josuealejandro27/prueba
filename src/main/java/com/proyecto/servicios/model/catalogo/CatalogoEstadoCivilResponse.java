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

    private Long id;
    private String nombre;
}
