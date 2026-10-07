package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Excepción personalizada para errores de negocio relacionados con clientes.
 */
@Getter
public class ClienteException extends RuntimeException {

    private final String code;

    public ClienteException(String code, String message) {
        super(message);
        this.code = code;
    }
}
