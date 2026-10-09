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
 * Catálogo de colonias.
 * Una colonia pertenece a un municipio.
 */
@Entity
@Table(name = "catalogo_colonia")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoColonia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la colonia es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre de la colonia no debe ser menor a 2 ni mayor a 100 caracteres")
    @Column(nullable = false, length = 100, name = "nombre", columnDefinition = "TEXT")
    private String nombre;

    @NotNull(message = "El municipio de la colonia es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "municipio_id", nullable = false)
    private CatalogoMunicipio municipio;
}