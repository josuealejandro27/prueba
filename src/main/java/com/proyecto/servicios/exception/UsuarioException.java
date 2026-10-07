package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Excepción personalizada para errores de negocio relacionados con usuarios.
 */
@Getter
public class UsuarioException extends RuntimeException {

    private final String code;

    public UsuarioException(String code, String message) {
        super(message);
        this.code = code;
    }
}
