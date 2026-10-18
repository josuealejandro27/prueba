package com.proyecto.servicios.model.clientes;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecto.servicios.model.pagination.PaginaRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criterios de búsqueda de clientes. Todos los filtros son opcionales y
 * COMBINABLES entre sí (se aplican con AND); sin filtros se devuelven todos
 * los clientes paginados. La paginación se configura con {@code pagina} y
 * {@code tamano} heredados de {@link PaginaRequest}.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteSearchRequest extends PaginaRequest {

    /** CURP exacta (se compara en mayúsculas). Filtro combinable. */
    @Size(max = 18, message = "La CURP no puede tener más de 18 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "PERJ900520HDFRPN09")
    private String curp;

    /** RFC exacto (se compara en mayúsculas). Filtro combinable. */
    @Size(max = 13, message = "El RFC no puede tener más de 13 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "PERJ900520AB1")
    private String rfc;

    /** Coincidencia parcial de correo (sin distinguir mayúsculas). Filtro combinable. */
    @Size(max = 100, message = "El correo electrónico no puede tener más de 100 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "juan.perez")
    private String correo;

    /** Coincidencia parcial de nombre (sin distinguir mayúsculas). Filtro combinable. */
    @Size(max = 50, message = "El nombre no puede tener más de 50 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Juan")
    private String nombre;

    /** Coincidencia parcial de apellido paterno (sin distinguir mayúsculas). Filtro combinable. */
    @Size(max = 50, message = "El apellido paterno no puede tener más de 50 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "Pérez")
    private String apellidoPaterno;

    /** Coincidencia parcial de apellido materno (sin distinguir mayúsculas). Filtro combinable. */
    @Size(max = 50, message = "El apellido materno no puede tener más de 50 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "López")
    private String apellidoMaterno;

    /** Número de cuenta exacto. Filtro combinable. */
    @Size(max = 20, message = "El número de cuenta no puede tener más de 20 caracteres")
    @io.swagger.v3.oas.annotations.media.Schema(example = "012345678901234567")
    private String numeroCuenta;

    /** {@code true} = solo activos, {@code false} = solo inactivos, {@code null} = sin filtro. Filtro combinable. */
    @io.swagger.v3.oas.annotations.media.Schema(example = "true")
    private Boolean activo;

    /** {@code true} = solo cuentas bloqueadas, {@code false} = solo no bloqueadas, {@code null} = sin filtro. Filtro combinable. */
    @io.swagger.v3.oas.annotations.media.Schema(example = "false")
    private Boolean cuentaBloqueada;

    /** {@code true} = solo logins bloqueados, {@code false} = solo no bloqueados, {@code null} = sin filtro. Filtro combinable. */
    @io.swagger.v3.oas.annotations.media.Schema(example = "false")
    private Boolean loginBloqueado;

    /** Fecha mínima de creación del cliente (inclusive), formato yyyy-MM-dd. Filtro combinable. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "2026-01-01")
    private java.util.Date fechaInicio;

    /** Fecha máxima de creación del cliente (inclusive), formato yyyy-MM-dd. Filtro combinable. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @io.swagger.v3.oas.annotations.media.Schema(example = "2026-12-31")
    private java.util.Date fechaFin;
}
