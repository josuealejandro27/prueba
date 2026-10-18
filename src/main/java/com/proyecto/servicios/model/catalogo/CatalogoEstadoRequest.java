package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de creación de un estado (entidad federativa) del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoRequest {

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del estado no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;

    @NotNull(message = "El país del estado es obligatorio")
    @Positive(message = "El identificador del país debe ser un valor positivo")
    private Long paisId;
}
