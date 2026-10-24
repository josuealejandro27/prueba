package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando ya existe un usuario registrado con el mismo correo.
 * Responde 409 CONFLICT con una estructura JSON uniforme.
 */
public class CorreoDuplicadoException extends UsuarioException {

    private static final String CODIGO = "CORREO_DUPLICADO";

    public CorreoDuplicadoException(String correo) {
        super(CODIGO, "Ya existe un usuario con el correo: " + correo, HttpStatus.CONFLICT);
    }
}
