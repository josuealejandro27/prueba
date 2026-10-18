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

    private Long id;
    private String nombre;
    private Long paisId;
    private String paisNombre;
}
