package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO con el número de cuenta requerido para consultas puntuales
 * (consulta de cuenta y consulta de saldo).
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaNumeroRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(max = 30, message = "El número de cuenta no puede tener más de 30 caracteres")
    private String numeroCuenta;
}
