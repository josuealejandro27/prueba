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

    @io.swagger.v3.oas.annotations.media.Schema(example = "1")
    private Long id;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Juan")
    private String nombre;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Carlos")
    private String segundoNombre;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Pérez")
    private String apellidoPaterno;
    @io.swagger.v3.oas.annotations.media.Schema(example = "López")
    private String apellidoMaterno;
    @io.swagger.v3.oas.annotations.media.Schema(example = "1990-05-20")
    private Date fechaNacimiento;
    @io.swagger.v3.oas.annotations.media.Schema(example = "PERJ900520HDFRPN09")
    private String curp;
    @io.swagger.v3.oas.annotations.media.Schema(example = "PERJ900520AB1")
    private String rfc;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Masculino")
    private String genero;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Mexicana")
    private String nacionalidad;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Soltero")
    private String estadoCivil;
    @io.swagger.v3.oas.annotations.media.Schema(example = "juan.perez@email.com")
    private String correo;
    @io.swagger.v3.oas.annotations.media.Schema(example = "5512345678")
    private Long numeroTelefono;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Av. Insurgentes 123")
    private String calle;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Centro")
    private String colonia;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Cuernavaca")
    private String municipio;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Morelos")
    private String estado;
    @io.swagger.v3.oas.annotations.media.Schema(example = "62000")
    private String cp;
    @io.swagger.v3.oas.annotations.media.Schema(example = "México")
    private String pais;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Ing. en Sistemas")
    private String ocupacion;
    @io.swagger.v3.oas.annotations.media.Schema(example = "Mi Empresa SA")
    private String empresa;
    @io.swagger.v3.oas.annotations.media.Schema(example = "25000.00")
    private BigDecimal ingresoMensual;
    @io.swagger.v3.oas.annotations.media.Schema(example = "012345678901234567")
    private String numeroCuenta;
    @io.swagger.v3.oas.annotations.media.Schema(example = "15000.00")
    private BigDecimal saldo;
    @io.swagger.v3.oas.annotations.media.Schema(example = "false")
    private boolean cuentaBloqueada;
    @io.swagger.v3.oas.annotations.media.Schema(example = "false")
    private boolean loginBloqueado;
    @io.swagger.v3.oas.annotations.media.Schema(example = "true")
    private boolean activo;
}
