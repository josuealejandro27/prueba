package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para actualización parcial de cliente.
 * No incluye CURP, RFC ni número de cuenta (no modificables).
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    // Datos Personales (modificables)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El nombre no tiene que tener un valor menor a 2 y mayor a 50")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo puede contener letras y espacios")
    @Size(max = 50, message = "El segundo nombre no puede tener más de 50 caracteres")
    private String segundoNombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido paterno no tiene que tener un valor menor a 2 y mayor a 50")
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido materno no tiene que tener un valor menor a 2 y mayor a 50")
    private String apellidoMaterno;

    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private java.util.Date fechaNacimiento;

    private Long generoId;
    private Long nacionalidadId;
    private Long estadoCivilId;

    // Datos de Contacto (modificables)
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede tener más de 100 caracteres")
    private String correo;

    @Min(value = 1, message = "La LADA debe ser un valor positivo")
    @Max(value = 999, message = "La LADA no puede ser mayor a 999")
    private Short lada;

    @Min(value = 1000000000, message = "El número de teléfono debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono debe tener exactamente 10 dígitos")
    private Integer numeroTelefono;

    @Min(value = 1000000000, message = "El número de teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono alternativo debe tener exactamente 10 dígitos")
    private Integer numeroTelefono2;

    // Domicilio (modificable)
    @Size(max = 100, message = "La calle no puede tener más de 100 caracteres")
    private String calle;

    @Min(value = 1, message = "El número exterior debe ser un valor positivo")
    private Short noExterior;

    private Short noInterior;

    @Size(max = 100, message = "La colonia no puede tener más de 100 caracteres")
    private String colonia;

    @Size(max = 100, message = "El municipio no puede tener más de 100 caracteres")
    private String municipio;

    @Size(max = 100, message = "El estado no puede tener más de 100 caracteres")
    private String estado;

    @Min(value = 1000, message = "El código postal debe tener exactamente 5 dígitos")
    @Max(value = 99999, message = "El código postal debe tener exactamente 5 dígitos")
    private Integer cp;

    @Size(max = 100, message = "El país no puede tener más de 100 caracteres")
    private String pais;

    // Información Laboral (modificable)
    @Size(max = 100, message = "La ocupación no puede tener más de 100 caracteres")
    private String ocupacion;

    @Size(max = 250, message = "La empresa no puede tener más de 250 caracteres")
    private String empresa;

    @DecimalMin(value = "0.0", inclusive = false, message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 6, fraction = 2, message = "El ingreso mensual no puede tener más de 6 dígitos enteros y 2 decimales")
    private BigDecimal ingresoMensual;
}
