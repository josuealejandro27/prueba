package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando se intenta consultar o modificar un usuario que no existe.
 * Responde 404 NOT_FOUND con una estructura JSON uniforme.
 */
public class UsuarioNoEncontradoException extends UsuarioException {

    private static final String CODIGO = "USUARIO_NO_ENCONTRADO";

    public UsuarioNoEncontradoException(Long id) {
        super(CODIGO, "Usuario no encontrado con ID: " + id, HttpStatus.NOT_FOUND);
    }
}
