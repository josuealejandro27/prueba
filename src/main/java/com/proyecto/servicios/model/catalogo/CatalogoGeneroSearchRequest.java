package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de géneros del catálogo.
 * Todos los filtros son opcionales y combinables; sin criterios se devuelven todos.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoGeneroSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por tipo de género. */
    @Size(max = 50, message = "El tipo de género no puede tener más de 50 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Masculino")
    private String tipo;
}
