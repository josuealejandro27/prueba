package com.proyecto.servicios.model.clientes;

import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de cuentas bancarias.
 * Todos los filtros son opcionales y combinables; la paginación se hereda de
 * {@link PaginaRequest}. Sin filtros se devuelven todas las cuentas.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaSearchRequest extends PaginaRequest {

    /** Filtro por identificador del cliente (persona física). */
    @Positive(message = "El identificador del cliente debe ser un valor positivo")
    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long clienteId;

    /** {@code true} = solo cuentas ACTIVAS, {@code false} = solo no activas, {@code null} = sin filtro. */
    @io.swagger.v3.oas.annotations.media.Schema(example = "true")
    private Boolean activas;

    /** Filtro por estatus exacto (ACTIVA, BLOQUEADA, INACTIVA, CANCELADA). Filtro combinable. */
    @Size(max = 20, message = "El estatus no puede tener más de 20 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "ACTIVA")
    private String estatus;
}
