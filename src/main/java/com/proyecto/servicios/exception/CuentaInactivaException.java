package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaInactivaException extends CuentaException {

    private static final String CODIGO = "CUENTA_NO_ACTIVA";

    public CuentaInactivaException(String numeroCuenta, String estatusActual) {
        super(CODIGO, String.format("La cuenta %s no se encuentra ACTIVA (estatus actual: %s)", numeroCuenta, estatusActual),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

