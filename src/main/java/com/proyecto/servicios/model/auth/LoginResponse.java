package com.proyecto.servicios.model.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo = "Bearer";
    private Long usuarioId;
    private String correo;
    private String nombre;
}
