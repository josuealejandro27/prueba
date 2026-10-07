package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InformacionLaboral {

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "ocupacion", columnDefinition = "TEXT")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 250, message = "La empresa no puede tener más de 250 caracteres")
    @Column(nullable = false, name = "empresa", length = 250, columnDefinition = "TEXT")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 6, fraction = 2, message = "El ingreso mensual no puede tener más de 6 dígitos enteros y 2 decimales")
    @Column(nullable = false, name = "ingreso_mensual", precision = 8, scale = 2)
    private BigDecimal ingresoMensual;

    @NotNull(message = "El número de teléfono es obligatorio")
    @Min(value = 1000000000, message = "El número de teléfono debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono debe tener exactamente 10 dígitos")
    @Column(nullable = false, name = "numero_telefono", length = 10)
    private Integer numeroTelefono;
}
