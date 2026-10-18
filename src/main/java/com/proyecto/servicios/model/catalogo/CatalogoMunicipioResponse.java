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

    private Long id;
    private String nombre;
    private Long estadoId;
    private String estadoNombre;
}
