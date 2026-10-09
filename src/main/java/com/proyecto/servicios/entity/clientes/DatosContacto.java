package com.proyecto.servicios.entity.clientes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatosContacto {

    @Column(nullable = false,name = "correo",length = 100,columnDefinition = "TEXT")
    private String correo;

    @Column(nullable = false, name = "lada", length = 3)
    private Short lada;

    @Column(nullable = false, name = "numeroTelefono", length = 10)
    private Integer numeroTelefono;

    @Column(nullable = true, name = "numeroTelefono2", length = 10)
    private Integer numeroTelefono2;
}
