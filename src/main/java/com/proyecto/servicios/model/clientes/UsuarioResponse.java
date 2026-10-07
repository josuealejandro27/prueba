package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioResponse {

    private Long id;
    private Long clienteId;
    private String correo;
    private boolean activo;
    private Date fechaCreacion;
    private Date fechaActualizacion;
}
