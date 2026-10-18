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

    private Long id;
    private String nombre;
}
