package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domicilio")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_fisica_id", nullable = false, unique = true)
    private PersonaFisica personaFisica;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "calle", length = 100, columnDefinition = "TEXT")
    private String calle;

    @NotNull(message = "El número exterior es obligatorio")
    @Min(value = 1, message = "El número exterior debe ser un valor positivo")
    @Column(nullable = false, name = "no_exterior", length = 10)
    private Short noExterior;

    @Column(nullable = true, name = "no_interior", length = 10)
    private Short noInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "colonia", columnDefinition = "TEXT")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "municipio", columnDefinition = "TEXT")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "estado", columnDefinition = "TEXT")
    private String estado;

    @NotNull(message = "El código postal es obligatorio")
    @Min(value = 1000, message = "El código postal debe tener exactamente 5 dígitos")
    @Max(value = 99999, message = "El código postal debe tener exactamente 5 dígitos")
    @Column(nullable = false, name = "cp", length = 5)
    private Integer cp;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no puede tener más de 100 caracteres")
    @Column(nullable = false, name = "pais", columnDefinition = "TEXT")
    private String pais;
}
