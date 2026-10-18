package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción personalizada para errores de negocio relacionados con personas.
 */
@Getter
public class PersonaException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public PersonaException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public PersonaException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
