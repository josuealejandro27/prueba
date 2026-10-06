package com.proyecto.servicios.entity.clientes;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name="PersonaFisica")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonaFisica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, name = "nombre", columnDefinition = "TEXT")
    private String nombre;

    @Column(nullable = true, length = 50, name = "segundoNombre", columnDefinition = "TEXT")
    private String segundoNombre;

    @Column(nullable = false, length = 50, name = "apellidoPaterno", columnDefinition = "TEXT")
    private String apellidoPaterno;

    @Column(nullable = false, length = 50, name = "apellidoMaterno", columnDefinition = "TEXT")
    private String getApellidoMaterno;

    @Column(nullable = false, name = "FechaNacimiento")
    private Date fechaNacimiento;

    @Column(nullable = false, length = 18, name = "curp", columnDefinition = "TEXT")
    private String curp;

    @Column(nullable = false, length = 13, name = "rfc", columnDefinition = "TEXT")
    private String rfc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genero_id", nullable = false)
    private CatalogoGeneros genero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nacionalidad_id", nullable = false)
    private CatalogoPais nacionalidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estadoCivil_id", nullable = false)
    private CatalogoEstadoCivil estadoCivil;

    //Datos de contacto
    @Column(nullable = false,name = "correo",length = 100,columnDefinition = "TEXT")
    private String correo;

    @Column(nullable = false, name = "lada", length = 3)
    private Short lada;

    @Column(nullable = false, name = "numeroTelefono", length = 10)
    private Integer numeroTelefono;

    @Column(nullable = true, name = "numeroTelefono2", length = 10)
    private Integer numeroTelefono2;

    @Embedded
    private DatosContacto datosContacto;

    //Domicilio
    @Embedded
    private Domicilio domicilio;

    //Información Laboral
    @Embedded
    private InformacionLaboral informacionLaboral;

    // Estado de la cuenta
    @Column(nullable = false, name = "cuenta_bloqueada")
    private boolean cuentaBloqueada;

    // Estado del login
    @Column(nullable = false, name = "login_bloqueado")
    private boolean loginBloqueado;
}
