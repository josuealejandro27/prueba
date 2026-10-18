package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para la creación de un estado civil del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoCivilRequest {

    @NotBlank(message = "El nombre del estado civil es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del estado civil no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;
}
