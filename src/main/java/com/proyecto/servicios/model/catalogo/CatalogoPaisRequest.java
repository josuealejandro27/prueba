package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para la creación de un país del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoPaisRequest {

    @NotBlank(message = "El nombre del país es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del país no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;
}
