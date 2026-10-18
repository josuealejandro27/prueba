package com.proyecto.servicios.model.clientes;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de usuarios. Todos los campos son opcionales y combinables.
 * Sin filtros se devuelven todos los usuarios. Hereda la paginación configurable
 * ({@code pagina} y {@code tamano}) de {@link PaginaRequest}.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioSearchRequest extends PaginaRequest {

    /** Criterio de búsqueda parcial por correo (no requiere un correo completo). */
    @Size(max = 100, message = "El correo electrónico no puede tener más de 100 caracteres")
    private String correo;

    @Positive(message = "El identificador del cliente debe ser un valor positivo")
    private Long clienteId;

    /** {@code true} = solo activos, {@code false} = solo inactivos, {@code null} = sin filtro. */
    private Boolean activos;
}
