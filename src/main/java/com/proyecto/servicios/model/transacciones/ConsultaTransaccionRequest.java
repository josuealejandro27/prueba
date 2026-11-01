package com.proyecto.servicios.model.transacciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaTransaccionRequest {

    @Schema(description = "Folio único de la transacción o clave de idempotencia a consultar", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    @NotBlank(message = "El folio o clave de la transacción es obligatorio")
    private String folio;
}

