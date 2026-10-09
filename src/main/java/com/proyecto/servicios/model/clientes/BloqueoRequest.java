package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BloqueoRequest {

    @NotNull(message = "El estado de bloqueo es obligatorio")
    private boolean bloqueado;
}
