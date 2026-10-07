package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Excepción personalizada para errores de negocio relacionadas con cuentas bancarias.
 */
@Getter
public class CuentaException extends RuntimeException {

    private final String code;

    public CuentaException(String code, String message) {
        super(message);
        this.code = code;
    }
}
