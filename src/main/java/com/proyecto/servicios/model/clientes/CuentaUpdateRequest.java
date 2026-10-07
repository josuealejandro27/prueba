package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para actualización parcial de cuenta bancaria.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaUpdateRequest {

    @DecimalMin(value = "0.0", message = "El saldo no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El saldo no puede tener más de 8 dígitos enteros y 2 decimales")
    private BigDecimal saldo;

    @Pattern(regexp = "^(ACTIVA|BLOQUEADA|CANCELADA)$", message = "El estatus debe ser ACTIVA, BLOQUEADA o CANCELADA")
    private String estatus;
}
