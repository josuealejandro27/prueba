package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Date fechaNacimiento;
    private String curp;
    private String rfc;
    private String genero;
    private String nacionalidad;
    private String estadoCivil;
    private String correo;
    private Long numeroTelefono;
    private String calle;
    private String colonia;
    private String municipio;
    private String estado;
    private String cp;
    private String pais;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private String numeroCuenta;
    private BigDecimal saldo;
    private boolean cuentaBloqueada;
    private boolean loginBloqueado;
    private boolean activo;
}
