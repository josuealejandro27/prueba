package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de nacionalidades.
 */
@Entity
@Table(name = "catalogo_nacionalidad")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoNacionalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la nacionalidad es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre de la nacionalidad no debe ser menor a 2 ni mayor a 100 caracteres")
    @Column(nullable = false, length = 100, name = "nombre", columnDefinition = "TEXT")
    private String nombre;
}