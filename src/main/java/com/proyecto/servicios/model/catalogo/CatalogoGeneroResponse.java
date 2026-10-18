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

    private Long id;
    private String tipo;
}
