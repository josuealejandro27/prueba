package com.proyecto.servicios.model.transacciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequest {

    @Schema(description = "Número de cuenta bancaria de 16 dígitos", example = "1234567890123456")
    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(min = 16, max = 16, message = "El número de cuenta debe tener exactamente 16 caracteres")
    private String numeroCuenta;

    @Schema(description = "Identificador del servicio en Gestopago", example = "105")
    @NotNull(message = "El identificador del servicio es obligatorio")
    @Positive(message = "El identificador del servicio debe ser un número positivo")
    private Long idServicio;

    @Schema(description = "Identificador del producto en Gestopago", example = "201")
    @NotNull(message = "El identificador del producto es obligatorio")
    @Positive(message = "El identificador del producto debe ser un número positivo")
    private Long idProducto;

    @Schema(description = "Monto de la transacción", example = "99.90")
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a pagar debe ser mayor a 0")
    @Digits(integer = 8, fraction = 2, message = "El monto no puede tener más de 8 enteros y 2 decimales")
    private BigDecimal monto;

    @Schema(description = "Identificador del distribuidor asignado por Gestopago", example = "83")
    @NotNull(message = "El identificador del distribuidor es obligatorio")
    @Positive(message = "El distribuidor debe ser positivo")
    private Integer idDistribuidor;

    @Schema(description = "Número telefónico o referencia del servicio a pagar", example = "5512345678")
    @NotBlank(message = "El teléfono o referencia es obligatorio")
    @Size(max = 100, message = "La referencia no puede tener más de 100 caracteres")
    private String referencia;

    @Schema(description = "Indicador operativo del producto", example = "T")
    @NotBlank(message = "La bandera del producto es obligatoria")
    private String bandera;

    @Schema(description = "PIN o clave de seguridad para autorizar la transacción", example = "Password123!")
    @NotBlank(message = "El PIN o clave de seguridad es obligatorio")
    @Size(min = 4, max = 100, message = "El PIN debe tener entre 4 y 100 caracteres")
    private String pin;

    @Schema(description = "Código UPC del producto (opcional)", example = "123456789012")
    @Size(max = 50, message = "El UPC no puede superar 50 caracteres")
    private String upc;

    @Schema(description = "Clave única de idempotencia generada por el cliente", example = "tx-cli-20261028-001")
    @NotBlank(message = "La clave de idempotencia es obligatoria para garantizar la integridad de la operación")
    @Size(max = 100, message = "La clave de idempotencia no puede superar 100 caracteres")
    private String idempotencyKey;
}

