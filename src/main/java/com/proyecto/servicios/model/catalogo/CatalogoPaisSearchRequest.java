package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de países del catálogo.
 * Todos los filtros son opcionales y combinables; sin criterios se devuelven todos.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoPaisSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por nombre del país. */
    @Size(max = 100, message = "El nombre del país no puede tener más de 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "México")
    private String nombre;
}
