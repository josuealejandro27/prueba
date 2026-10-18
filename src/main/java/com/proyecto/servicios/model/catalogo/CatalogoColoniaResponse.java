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

    private Long id;
    private String nombre;
    private Long municipioId;
    private String municipioNombre;
}
