package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de estados / entidades federativas.
 * Un estado pertenece a un país.
 */
@Entity
@Table(name = "catalogo_estado")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del estado no debe ser menor a 2 ni mayor a 100 caracteres")
    @Column(nullable = false, length = 100, name = "nombre", columnDefinition = "TEXT")
    private String nombre;

    @NotNull(message = "El país del estado es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pais_id", nullable = false)
    private CatalogoPais pais;
}