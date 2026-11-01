package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class TransaccionIdempotenciaException extends CuentaException {

    private static final String CODIGO = "TRANSACCION_EN_PROCESO";

    public TransaccionIdempotenciaException(String idempotencyKey) {
        super(CODIGO, "Ya existe una transacción en proceso con la clave de idempotencia: " + idempotencyKey,
                HttpStatus.CONFLICT);
    }
}

