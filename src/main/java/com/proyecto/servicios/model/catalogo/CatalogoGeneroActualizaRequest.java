package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para la actualización de un género del catálogo.
 * El identificador viaja dentro del cuerpo de la petición.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoGeneroActualizaRequest {

    @NotNull(message = "El identificador es obligatorio")
    @Positive(message = "El identificador debe ser un valor positivo")
    private Long id;

    @NotBlank(message = "El tipo de género es obligatorio")
    @Size(min = 2, max = 50, message = "El tipo de género no debe ser menor a 2 ni mayor a 50 caracteres")
    private String tipo;
}
