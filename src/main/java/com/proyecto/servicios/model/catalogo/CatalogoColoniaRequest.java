package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de creación de una colonia del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoColoniaRequest {

    @NotBlank(message = "El nombre de la colonia es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre de la colonia no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;

    @NotNull(message = "El municipio de la colonia es obligatorio")
    @Positive(message = "El identificador del municipio debe ser un valor positivo")
    private Long municipioId;
}
