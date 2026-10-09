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
 * Catálogo de municipios.
 * Un municipio pertenece a un estado.
 */
@Entity
@Table(name = "catalogo_municipio")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoMunicipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del municipio es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del municipio no debe ser menor a 2 ni mayor a 100 caracteres")
    @Column(nullable = false, length = 100, name = "nombre", columnDefinition = "TEXT")
    private String nombre;

    @NotNull(message = "El estado del municipio es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private CatalogoEstado estado;
}