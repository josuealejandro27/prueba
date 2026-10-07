package com.proyecto.servicios.entity.clientes;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "persona_fisica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El nombre no tiene que tener un valor menor a 2 y mayor a 50")
    @Column(nullable = false, length = 50, name = "nombre", columnDefinition = "TEXT")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo puede contener letras y espacios")
    @Size(max = 50, message = "El segundo nombre no puede tener más de 50 caracteres")
    @Column(nullable = true, length = 50, name = "segundo_nombre", columnDefinition = "TEXT")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido paterno no tiene que tener un valor menor a 2 y mayor a 50")
    @Column(nullable = false, length = 50, name = "apellido_paterno", columnDefinition = "TEXT")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo puede contener letras y espacios")
    @Size(min = 2, max = 50, message = "El apellido materno no tiene que tener un valor menor a 2 y mayor a 50")
    @Column(nullable = false, length = 50, name = "apellido_materno", columnDefinition = "TEXT")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @Column(nullable = false, name = "fecha_nacimiento")
    private Date fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9]{2}$", message = "La CURP no tiene un formato válido")
    @Column(nullable = false, length = 18, name = "curp", columnDefinition = "TEXT", unique = true)
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[A-Z0-9]{3}$", message = "El RFC no tiene un formato válido")
    @Column(nullable = false, length = 13, name = "rfc", columnDefinition = "TEXT", unique = true)
    private String rfc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genero_id", nullable = false)
    private CatalogoGeneros genero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nacionalidad_id", nullable = false)
    private CatalogoPais nacionalidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_civil_id", nullable = false)
    private CatalogoEstadoCivil estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "correo", length = 100, columnDefinition = "TEXT", unique = true)
    private String correo;

    @NotNull(message = "La LADA es obligatoria")
    @Min(value = 1, message = "La LADA debe ser un valor positivo")
    @Max(value = 999, message = "La LADA no puede ser mayor a 999")
    @Column(nullable = false, name = "lada", length = 3)
    private Short lada;

    @NotNull(message = "El número de teléfono es obligatorio")
    @Min(value = 1000000000, message = "El número de teléfono debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono debe tener exactamente 10 dígitos")
    @Column(nullable = false, name = "numero_telefono", length = 10)
    private Integer numeroTelefono;

    @Min(value = 1000000000, message = "El número de teléfono alternativo debe tener exactamente 10 dígitos")
    @Max(value = 9999999999L, message = "El número de teléfono alternativo debe tener exactamente 10 dígitos")
    @Column(nullable = true, name = "numero_telefono2", length = 10)
    private Integer numeroTelefono2;

    @Embedded
    private Domicilio domicilio;

    @Embedded
    private InformacionLaboral informacionLaboral;

    @Column(nullable = false, name = "cuenta_bloqueada")
    private boolean cuentaBloqueada;

    @Column(nullable = false, name = "login_bloqueado")
    private boolean loginBloqueado;

    @Column(nullable = false, name = "activo")
    private boolean activo;

    @Column(nullable = false, name = "fecha_creacion", updatable = false)
    private Date fechaCreacion;

    @Column(nullable = false, name = "fecha_actualizacion")
    private Date fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = new Date();
        fechaActualizacion = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = new Date();
    }
}
