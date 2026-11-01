package com.proyecto.servicios.entity.transacciones;

import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.TipoOperacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entidad que registra y audita el ciclo de vida de cada transacción financiera.
 */
@Entity
@Table(name = "transacciones_financieras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionFinanciera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 36)
    private String folio;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100, name = "idempotency_key")
    private String idempotencyKey;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaBancaria cuenta;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "tipo_operacion")
    private TipoOperacion tipoOperacion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTransaccion estado;

    @NotNull
    @Column(nullable = false, name = "id_distribuidor")
    private Integer idDistribuidor;

    @Column(name = "id_servicio")
    private Long idServicio;

    @Column(name = "id_producto")
    private Long idProducto;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String referencia;

    @Column(length = 20, name = "codigo_respuesta_proveedor")
    private String codigoRespuestaProveedor;

    @Column(columnDefinition = "TEXT", name = "mensaje_proveedor")
    private String mensajeProveedor;

    @Column(length = 100, name = "folio_proveedor")
    private String folioProveedor;

    @Column(nullable = false, name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false, name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
        if (monto != null) {
            monto = monto.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
        if (monto != null) {
            monto = monto.setScale(2, RoundingMode.HALF_UP);
        }
    }
}

