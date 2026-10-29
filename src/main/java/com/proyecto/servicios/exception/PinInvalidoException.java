package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class PinInvalidoException extends CuentaException {

    private static final String CODIGO = "PIN_INVALIDO";

    public PinInvalidoException() {
        super(CODIGO, "El PIN o clave de seguridad proporcionado es incorrecto", HttpStatus.UNAUTHORIZED);
    }
}

