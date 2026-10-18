package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para bloquear o desbloquear la cuenta / el login de un cliente.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteBloqueoRequest {

    @NotNull(message = "El identificador del cliente es obligatorio")
    @Positive(message = "El identificador del cliente debe ser un valor positivo")
    private Long id;

    @NotNull(message = "El estado de bloqueo es obligatorio")
    private Boolean bloqueado;
}
