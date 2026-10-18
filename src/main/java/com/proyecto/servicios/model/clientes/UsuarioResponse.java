package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long id;
    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long clienteId;
    @io.swagger.v3.oas.annotations.media.Schema(example = "juan.perez@email.com")
    private String correo;
    @io.swagger.v3.oas.annotations.media.Schema(example = "true")
    private boolean activo;
    @io.swagger.v3.oas.annotations.media.Schema(example = "2026-01-15T10:30:00")
    private Date fechaCreacion;
    @io.swagger.v3.oas.annotations.media.Schema(example = "2026-01-15T10:30:00")
    private Date fechaActualizacion;
}
