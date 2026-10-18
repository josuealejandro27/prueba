package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de municipios. Todos los filtros son opcionales y combinables.
 * Sin filtros se devuelven todos los municipios (paginados).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoMunicipioSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por nombre del municipio (no requiere el nombre completo). */
    @Size(max = 100, message = "El nombre del municipio no debe ser mayor a 100 caracteres")
    private String nombre;

    @Positive(message = "El identificador del estado debe ser un valor positivo")
    private Long estadoId;
}
