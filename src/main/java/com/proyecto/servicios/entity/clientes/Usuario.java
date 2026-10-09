package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private PersonaFisica cliente;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "correo", length = 100, columnDefinition = "TEXT", unique = true)
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false, name = "password", length = 255)
    private String password;

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
