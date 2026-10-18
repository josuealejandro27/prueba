package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de actualización de un estado (entidad federativa) del catálogo.
 * Incluye el identificador del estado a actualizar.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoActualizaRequest {

    @NotNull(message = "El identificador del estado es obligatorio")
    @Positive(message = "El identificador del estado debe ser un valor positivo")
    private Long id;

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del estado no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;

    @NotNull(message = "El país del estado es obligatorio")
    @Positive(message = "El identificador del país debe ser un valor positivo")
    private Long paisId;
}
