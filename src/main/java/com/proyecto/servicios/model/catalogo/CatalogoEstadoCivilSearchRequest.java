package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de estados civiles del catálogo.
 * Todos los filtros son opcionales y combinables; sin criterios se devuelven todos.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoCivilSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por nombre del estado civil. */
    @Size(max = 100, message = "El nombre del estado civil no puede tener más de 100 caracteres")
    private String nombre;
}
