package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GenericResponse {
    @io.swagger.v3.oas.annotations.media.Schema(example = "200", description = "Código del resultado")
    private Integer codigo;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Operación exitosa", description = "Mensaje del resultado")
    private String mensaje;
}
