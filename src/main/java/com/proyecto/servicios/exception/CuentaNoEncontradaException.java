package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends CuentaException {

    private static final String CODIGO = "CUENTA_NO_ENCONTRADA";

    public CuentaNoEncontradaException(String numeroCuenta) {
        super(CODIGO, "No se encontró ninguna cuenta bancaria con el número: " + numeroCuenta, HttpStatus.NOT_FOUND);
    }
}

