package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de nacionalidades del catálogo.
 * Todos los filtros son opcionales y combinables; sin criterios se devuelven todas.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoNacionalidadSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por nombre de la nacionalidad. */
    @Size(max = 100, message = "El nombre de la nacionalidad no puede tener más de 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Mexicana")
    private String nombre;
}
