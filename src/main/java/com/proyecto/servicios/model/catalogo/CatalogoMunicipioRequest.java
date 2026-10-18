package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de creación de un municipio del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoMunicipioRequest {

    @NotBlank(message = "El nombre del municipio es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del municipio no debe ser menor a 2 ni mayor a 100 caracteres")
    private String nombre;

    @NotNull(message = "El estado del municipio es obligatorio")
    @Positive(message = "El identificador del estado debe ser un valor positivo")
    private Long estadoId;
}
