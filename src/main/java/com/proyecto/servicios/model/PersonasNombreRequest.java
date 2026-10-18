package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para consultar una persona por su nombre,
 * que es la llave natural utilizada por los endpoints legacy de personas.
 */
@Getter
@Setter
@NoArgsConstructor
public class PersonasNombreRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Juan")
    private String nombre;
}
