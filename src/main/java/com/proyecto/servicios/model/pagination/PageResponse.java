package com.proyecto.servicios.model.pagination;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Estructura uniforme para todas las respuestas paginadas del API.
 * Se construye a partir de un {@link Page} de Spring Data.
 */
@Getter
@Setter
@NoArgsConstructor
public class PageResponse<T> {

    @io.swagger.v3.oas.annotations.media.Schema(description = "Elementos de la página actual")
    private List<T> contenido;

    @io.swagger.v3.oas.annotations.media.Schema(example = "0", description = "Página actual consultada (base 0: 0 = primera)")
    private int pagina;

    @io.swagger.v3.oas.annotations.media.Schema(example = "10", description = "Elementos por página")
    private int tamano;

    @io.swagger.v3.oas.annotations.media.Schema(example = "25", description = "Total de elementos que cumplen los filtros")
    private long totalElementos;

    @io.swagger.v3.oas.annotations.media.Schema(example = "3", description = "Total de páginas disponibles")
    private int totalPaginas;

    @io.swagger.v3.oas.annotations.media.Schema(example = "false", description = "true si es la última página")
    private boolean esUltima;

    /**
     * Construye la respuesta paginada transformando cada elemento de la página
     * mediante {@code mapper} (por ejemplo, entidad -> DTO de respuesta).
     */
    public static <S, T> PageResponse<T> desde(Page<S> page, Function<S, T> mapper) {
        PageResponse<T> response = new PageResponse<>();
        response.contenido = page.getContent().stream().map(mapper).toList();
        response.pagina = page.getNumber();
        response.tamano = page.getSize();
        response.totalElementos = page.getTotalElements();
        response.totalPaginas = page.getTotalPages();
        response.esUltima = page.isLast();
        return response;
    }

    /**
     * Construye la respuesta paginada cuando los elementos ya son del tipo de salida.
     */
    public static <T> PageResponse<T> desde(Page<T> page) {
        return desde(page, Function.identity());
    }
}
