package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CuentaResponse {

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long id;
    @io.swagger.v3.oas.annotations.media.Schema(example = "012345678901234567")
    private String numeroCuenta;
    @io.swagger.v3.oas.annotations.media.Schema(example = "15000.00")
    private BigDecimal saldo;
    @io.swagger.v3.oas.annotations.media.Schema(example = "2026-01-15T10:30:00")
    private LocalDateTime fechaApertura;
    @io.swagger.v3.oas.annotations.media.Schema(example = "ACTIVA", description = "ACTIVA, BLOQUEADA, INACTIVA o CANCELADA")
    private String estatus;
    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long clienteId;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Juan Pérez López")
    private String nombreCliente;
}
