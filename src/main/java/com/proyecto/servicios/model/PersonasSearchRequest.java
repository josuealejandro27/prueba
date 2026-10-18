package com.proyecto.servicios.model;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de personas. Todos los campos son opcionales y combinables.
 * Sin filtros se devuelven todas las personas. Hereda la paginación configurable
 * ({@code pagina} y {@code tamano}) de {@link PaginaRequest}.
 */
@Getter
@Setter
@NoArgsConstructor
public class PersonasSearchRequest extends PaginaRequest {

    /** Coincidencia parcial por nombre (no requiere el nombre completo). */
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String nombre;

    /** Coincidencia parcial por apellido paterno. */
    @Size(max = 100, message = "El apellido paterno no puede tener más de 100 caracteres")
    private String apellidoP;

    /** Coincidencia parcial por apellido materno. */
    @Size(max = 100, message = "El apellido materno no puede tener más de 100 caracteres")
    private String apellidoMaterno;
}
