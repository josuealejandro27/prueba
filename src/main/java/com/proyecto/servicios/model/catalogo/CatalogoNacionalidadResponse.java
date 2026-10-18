package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta estandarizada de una nacionalidad del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoNacionalidadResponse {

    private Long id;
    private String nombre;
}
