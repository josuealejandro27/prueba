package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.units.qual.C;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Domicilio {

    @Column(nullable = false,name = "calle",length = 100,columnDefinition = "TEXT")
    private String calle;

    @Column(nullable = false,name = "noExterior",length = 10)
    private Short noExterior;

    @Column(nullable = true, name = "noInterior",length = 10)
    private Short noInterior;

    @Column(nullable = false, name = "colonia")
    private Long colonia;

    @Column(nullable = false, name = "municipio")
    private Long municipio;

    @Column(nullable = false, name = "estado")
    private Long estado;

    @Column(nullable = false, name = "cp", length = 5)
    private Integer cp;

    @Column(nullable = false, name = "pais")
    private Long pais;
}
