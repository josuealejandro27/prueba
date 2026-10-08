package com.proyecto.servicios.entity.catalogos;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalogo_genero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoGeneros {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El tipo de género es obligatorio")
    @Size(min = 2, max = 50, message = "El tipo de género no tiene que tener un valor menor a 2 y mayor a 50")
    @Column(nullable = false, length = 50, name = "tipo", columnDefinition = "TEXT")
    private String tipo;
}
