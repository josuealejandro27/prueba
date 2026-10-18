package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para la creación de un género del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoGeneroRequest {

    @NotBlank(message = "El tipo de género es obligatorio")
    @Size(min = 2, max = 50, message = "El tipo de género no debe ser menor a 2 ni mayor a 50 caracteres")
    private String tipo;
}
