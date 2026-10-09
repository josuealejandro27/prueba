package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuenta_bancaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuentaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(min = 16, max = 16, message = "El número de cuenta debe tener exactamente 16 caracteres")
    @Column(nullable = false, unique = true, length = 16, name = "numero_cuenta")
    private String numeroCuenta;

    @NotNull(message = "El saldo es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El saldo no puede tener más de 8 dígitos enteros y 2 decimales")
    @Column(nullable = false, precision = 10, scale = 2, name = "saldo")
    private BigDecimal saldo;

    @Column(nullable = false, name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_fisica_id", nullable = false)
    private PersonaFisica personaFisica;

    @NotBlank(message = "El estatus es obligatorio")
    @Pattern(regexp = "^(ACTIVA|BLOQUEADA|CANCELADA)$", message = "El estatus debe ser ACTIVA, BLOQUEADA o CANCELADA")
    @Column(nullable = false, name = "estatus", length = 20)
    private String estatus;

    @PrePersist
    @PreUpdate
    public void redondearSaldo() {
        if (this.saldo != null) {
            this.saldo = this.saldo.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
