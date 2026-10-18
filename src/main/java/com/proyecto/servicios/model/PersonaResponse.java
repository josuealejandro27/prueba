package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonaResponse  extends  GenericResponse{
    @io.swagger.v3.oas.annotations.media.Schema(example = "Juan")
    private String nombre;

    @io.swagger.v3.oas.annotations.media.Schema(example = "Pérez")
    private String apellidoP;

    @io.swagger.v3.oas.annotations.media.Schema(example = "López")
    private String apellidoMaterno;

}
