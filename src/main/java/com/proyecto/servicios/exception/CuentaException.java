package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción personalizada para errores de negocio relacionadas con cuentas bancarias.
 */
@Getter
public class CuentaException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public CuentaException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public CuentaException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
