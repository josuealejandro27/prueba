package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identificador de un registro de catálogo simple como criterio de entrada
 * de las operaciones que no reciben datos adicionales (consultar, eliminar).
 * Reutilizado por los catálogos de género, nacionalidad, país y estado civil.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoIdRequest {

    @NotNull(message = "El identificador es obligatorio")
    @Positive(message = "El identificador debe ser un valor positivo")
    private Long id;
}
