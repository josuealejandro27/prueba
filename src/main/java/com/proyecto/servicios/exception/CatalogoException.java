package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.Getter;

/**
 * Excepción controlada del dominio del catálogo de productos.
 * Transporta el {@link ApiResponseEnum} que define la respuesta estandarizada.
 */
@Getter
public class CatalogoException extends RuntimeException {

    private final ApiResponseEnum response;

    public CatalogoException(ApiResponseEnum response) {
        super(response.getDefaultMessage());
        this.response = response;
    }

    public CatalogoException(ApiResponseEnum response, String mensaje) {
        super(mensaje);
        this.response = response;
    }

    public CatalogoException(ApiResponseEnum response, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.response = response;
    }
}
