package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para actualización parcial de cuenta bancaria.
 * Solo expone campos que el cliente puede modificar; el saldo, las fechas de
 * apertura y el identificador son controlados por el servidor y no se reciben aquí.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaUpdateRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    private String numeroCuenta;

    @Pattern(regexp = "^(ACTIVA|BLOQUEADA|CANCELADA)$", message = "El estatus debe ser ACTIVA, BLOQUEADA o CANCELADA")
    private String estatus;
}
