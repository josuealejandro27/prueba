package com.proyecto.servicios.model.transacciones;

import com.proyecto.servicios.enums.EstadoTransaccion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {

    @Schema(description = "Folio único de la transacción", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String folio;

    @Schema(description = "Clave de idempotencia asociada", example = "tx-cli-20261028-001")
    private String idempotencyKey;

    @Schema(description = "Número de cuenta bancaria", example = "1234567890123456")
    private String numeroCuenta;

    @Schema(description = "Monto de la transacción", example = "99.90")
    private BigDecimal monto;

    @Schema(description = "Estado actual de la transacción", example = "EXITOSA")
    private EstadoTransaccion estado;

    @Schema(description = "Código de respuesta retornado por el proveedor Gestopago", example = "0")
    private String codigoRespuestaProveedor;

    @Schema(description = "Mensaje o descripción retornada por el proveedor", example = "TRANSACCION EXITOSA")
    private String mensajeProveedor;

    @Schema(description = "Folio o ticket de autorización retornado por el proveedor", example = "AUT-987654")
    private String folioProveedor;

    @Schema(description = "Saldo disponible restante en la cuenta bancaria", example = "400.10")
    private BigDecimal saldoRestante;

    @Schema(description = "Fecha y hora de la transacción", example = "2026-10-28T22:15:00")
    private LocalDateTime fechaTransaccion;
}

