package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InformacionLaboral {

    @Column(nullable = false,name = "ocupacion",columnDefinition = "TEXT")
    private String ocupacion;

    @Column(nullable = false,name = "empresa",length = 250,columnDefinition = "TEXT")
    private String empresa;

    @Column(nullable = false, name = "ingresoMensual", precision = 8, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(nullable = false, name = "numeroTelefono", length = 10)
    private Integer numeroTelefono;
}
