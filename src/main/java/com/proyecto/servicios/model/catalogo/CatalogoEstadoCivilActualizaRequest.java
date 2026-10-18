package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para la actualización de un estado civil del catálogo.
 * El identificador viaja dentro del cuerpo de la petición.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoEstadoCivilActualizaRequest {

    @NotNull(message = "El identificador es obligatorio")
    @Positive(message = "El identificador debe ser un valor positivo")
    private Long id;

    @NotBlank(message = "El nombre del estado civil es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del estado civil no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;
}
