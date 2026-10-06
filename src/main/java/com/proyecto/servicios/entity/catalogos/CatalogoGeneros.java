package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CatalogoGenero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoGeneros {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, name = "tipo", columnDefinition = "TEXT")
    private String tipo;
}
