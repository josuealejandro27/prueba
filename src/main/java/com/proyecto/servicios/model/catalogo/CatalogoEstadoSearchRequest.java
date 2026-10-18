package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de estados. Todos los filtros son opcionales y combinables.
 * Sin filtros se devuelven todos los estados (paginados).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por nombre del estado (no requiere el nombre completo). */
    @Size(max = 100, message = "El nombre del estado no debe ser mayor a 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Morelos")
    private String nombre;

    @Positive(message = "El identificador del país debe ser un valor positivo")
    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long paisId;
}
