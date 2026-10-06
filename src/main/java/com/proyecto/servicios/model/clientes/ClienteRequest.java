package com.proyecto.servicios.model.clientes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    // Datos Personales
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Date fechaNacimiento;
    private String curp;
    private String rfc;
    private Long generoId;
    private Long nacionalidadId;
    private Long estadoCivilId;

    // Datos de Contacto
    private String correo;
    private Short lada;
    private Integer numeroTelefono;
    private Integer numeroTelefono2;

    // Domicilio
    private String calle;
    private Short noExterior;
    private Short noInterior;
    private Long colonia;
    private Long municipio;
    private Long estado;
    private Integer cp;
    private Long pais;

    // Información Laboral
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;

    // Cuenta Bancaria
    private BigDecimal saldoInicial;
}
