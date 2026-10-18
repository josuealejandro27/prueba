package com.proyecto.servicios.model.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Parámetros comunes de paginación para las consultas.
 * {@code pagina} es base 0 (0 = primera página) y {@code tamano} es el número de
 * elementos por página. Ambos se envían en el cuerpo (DTO) junto con los filtros,
 * por ejemplo: {@code {"pagina":1,"tamano":10}}. Son opcionales: si no se envían
 * se aplica página 0 con 10 elementos. Ningún campo de paginación es obligatorio.
 */
@Getter
@Setter
@NoArgsConstructor
public class PaginaRequest {

    @io.swagger.v3.oas.annotations.media.Schema(example = "0", description = "Página a consultar (base 0: 0 = primera). Opcional, por defecto 0")
    @Min(value = 0, message = "El número de página no puede ser negativo")
    private Integer pagina = 0;

    @io.swagger.v3.oas.annotations.media.Schema(example = "10", description = "Elementos por página (1 a 100). Opcional, por defecto 10")
    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 100, message = "El tamaño de página no puede ser mayor a 100")
    private Integer tamano = 10;

    /**
     * Convierte los parámetros de paginación en un {@link Pageable} de Spring Data,
     * aplicando valores por defecto cuando no se informan.
     */
    public Pageable aPageable() {
        int numeroPagina = pagina == null ? 0 : pagina;
        int elementosPagina = tamano == null ? 10 : tamano;
        return PageRequest.of(numeroPagina, elementosPagina);
    }
}
