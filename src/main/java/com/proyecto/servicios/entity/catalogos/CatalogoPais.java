package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalogo_pais")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoPais {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, name = "nombre", columnDefinition = "TEXT")
    private String nombre;

    // La tabla catalogo_pais no contiene esta jerarquía; no forma parte del mapeo JPA.
    @Transient
    private CatalogoEstado catalogoEstado;
}
