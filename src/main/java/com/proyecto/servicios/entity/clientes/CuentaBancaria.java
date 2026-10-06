package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
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

    @Column(nullable = false, unique = true, length = 16, name = "numero_cuenta")
    private String numeroCuenta;

    @Column(nullable = false, precision = 10, scale = 2, name = "saldo")
    private BigDecimal saldo;

    @Column(nullable = false, name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_fisica_id", nullable = false)
    private PersonaFisica personaFisica;

    @PrePersist
    @PreUpdate
    public void redondearSaldo() {
        if (this.saldo != null) {
            this.saldo = this.saldo.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
