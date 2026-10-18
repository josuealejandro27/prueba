package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de creación de cuenta bancaria. El número de cuenta es opcional:
 * si no se envía, el servicio genera uno automáticamente.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaCreateRequest {

    @NotNull(message = "El identificador del cliente es obligatorio")
    @Positive(message = "El identificador del cliente debe ser un valor positivo")
    private Long clienteId;

    @Size(max = 30, message = "El número de cuenta no puede tener más de 30 caracteres")
    private String numeroCuenta;
}
