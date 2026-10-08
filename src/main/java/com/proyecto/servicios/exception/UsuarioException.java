package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción personalizada para errores de negocio relacionados con usuarios.
 */
@Getter
public class UsuarioException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public UsuarioException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public UsuarioException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
