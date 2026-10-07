package com.proyecto.servicios.model.clientes;

import jakarta.validation.constraints.*;
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
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El nombre no tiene que tener un valor menor a 2 y mayor a 50")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo puede contener letras y espacios")
    @Size(max = 50, message = "El segundo nombre no puede tener más de 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido paterno no tiene que tener un valor menor a 2 y mayor a 50")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido materno no tiene que tener un valor menor a 2 y mayor a 50")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private Date fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9]{2}$", message = "La CURP no tiene un formato válido")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[A-Z0-9]{3}$", message = "El RFC no tiene un formato válido")
    private String rfc;

    @NotNull(message = "El género es obligatorio")
    private Long generoId;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Long nacionalidadId;

    @NotNull(message = "El estado civil es obligatorio")
    private Long estadoCivilId;

    // Datos de Contacto
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede tener más de 100 caracteres")
    private String correo;

    @NotNull(message = "La LADA es obligatoria")
    @Min(value = 1, message = "La LADA debe ser un valor positivo")
    @Max(value = 999, message = "La LADA no puede ser mayor a 999")
    private Short lada;

    @NotNull(message = "El número de teléfono es obligatorio")
    @Min(value = 1000000000, message = "El número de teléfono debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono debe tener exactamente 10 dígitos")
    private Integer numeroTelefono;

    @Min(value = 1000000000, message = "El número de teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono alternativo debe tener exactamente 10 dígitos")
    private Integer numeroTelefono2;

    // Domicilio
    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede tener más de 100 caracteres")
    private String calle;

    @NotNull(message = "El número exterior es obligatorio")
    @Min(value = 1, message = "El número exterior debe ser un valor positivo")
    private Short noExterior;

    private Short noInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no puede tener más de 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no puede tener más de 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no puede tener más de 100 caracteres")
    private String estado;

    @NotNull(message = "El código postal es obligatorio")
    @Min(value = 1000, message = "El código postal debe tener exactamente 5 dígitos")
    @Max(value = 99999, message = "El código postal debe tener exactamente 5 dígitos")
    private Integer cp;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no puede tener más de 100 caracteres")
    private String pais;

    // Información Laboral
    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación no puede tener más de 100 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 250, message = "La empresa no puede tener más de 250 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 6, fraction = 2, message = "El ingreso mensual no puede tener más de 6 dígitos enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    // Cuenta Bancaria
    @NotNull(message = "El saldo inicial es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo inicial no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El saldo inicial no puede tener más de 8 dígitos enteros y 2 decimals")
    private BigDecimal saldoInicial;

    // Contraseña para el usuario
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "La contraseña debe tener al menos 8 caracteres, una letra mayúscula, una letra minúscula, un número y un carácter especial")
    private String password;
}
