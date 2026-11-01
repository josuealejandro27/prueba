package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends CuentaException {

    private static final String CODIGO = "SALDO_INSUFICIENTE";

    public SaldoInsuficienteException(String numeroCuenta, BigDecimal saldoActual, BigDecimal montoRequerido) {
        super(CODIGO, String.format("Saldo insuficiente en la cuenta %s. Saldo actual: %s, monto requerido: %s",
                numeroCuenta, saldoActual, montoRequerido), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

