package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para la creación de una nacionalidad del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoNacionalidadRequest {

    @NotBlank(message = "El nombre de la nacionalidad es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre de la nacionalidad no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;
}
