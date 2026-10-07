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

    private Long id;
    private String numeroCuenta;
    private BigDecimal saldo;
    private LocalDateTime fechaApertura;
    private String estatus;
    private Long clienteId;
    private String nombreCliente;
}
