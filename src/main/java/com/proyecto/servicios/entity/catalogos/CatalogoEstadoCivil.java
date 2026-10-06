package com.proyecto.servicios.entity.catalogos;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CatalogoEstadoCivil")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoEstadoCivil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, name = "nombre", columnDefinition = "TEXT")
    private String nombre;
}
