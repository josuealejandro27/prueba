package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Excepción de validación con código y mensaje personalizado.
 * Se lanza cuando un campo no cumple las reglas de negocio.
 */
@Getter
public class ValidationException extends RuntimeException {

    private final String code;

    public ValidationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
