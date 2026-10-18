package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO reutilizable con el identificador de un cliente,
 * para endpoints que sólo requieren el id en el cuerpo.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteIdRequest {

    @NotNull(message = "El identificador del cliente es obligatorio")
    @Positive(message = "El identificador del cliente debe ser un valor positivo")
    private Long id;
}
