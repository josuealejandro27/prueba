package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de colonias. Todos los filtros son opcionales y combinables.
 * Sin filtros se devuelven todas las colonias (paginadas).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoColoniaSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por nombre de la colonia (no requiere el nombre completo). */
    @Size(max = 100, message = "El nombre de la colonia no debe ser mayor a 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Centro")
    private String nombre;

    @Positive(message = "El identificador del municipio debe ser un valor positivo")
    @io.swagger.v3.oas.annotations.media.Schema(example = "37")
    private Long municipioId;
}
