package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identificador del usuario como criterio de entrada de las operaciones
 * que no reciben datos adicionales (consultar, activar, desactivar).
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioIdRequest {

    @NotNull(message = "El identificador del usuario es obligatorio")
    @Positive(message = "El identificador del usuario debe ser un valor positivo")
    private Long id;
}
