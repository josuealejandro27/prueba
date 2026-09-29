package com.proyecto.servicios.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catálogo de respuestas estandarizadas del API.
 * Centraliza código HTTP, código de negocio y mensaje por defecto.
 */
@Getter
@RequiredArgsConstructor
public enum ApiResponseEnum {

    OK("200", "200", "Operación exitosa", HttpStatus.OK),
    CATALOGO_NO_DISPONIBLE("CAT001", "404", "El catálogo de productos no se encuentra disponible en caché", HttpStatus.NOT_FOUND),
    ERROR_INTERNO("ERR001", "500", "Ocurrió un error interno al consultar el catálogo de productos", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_COMUNICACION("ERR002", "503", "No fue posible comunicarse con el servicio externo de productos", HttpStatus.SERVICE_UNAVAILABLE),
    ERROR_AUTENTICACION("ERR003", "401", "La autenticación hacia el servicio externo fue rechazada", HttpStatus.UNAUTHORIZED),
    TIMEOUT_SERVICIO("ERR004", "504", "El servicio externo de productos no respondió a tiempo", HttpStatus.GATEWAY_TIMEOUT);

    /** Código de negocio expuesto al cliente. */
    private final String code;

    /** Código HTTP como texto, útil para logs y trazas. */
    private final String httpCode;

    /** Mensaje por defecto de la respuesta. */
    private final String defaultMessage;

    /** Estado HTTP asociado a la respuesta. */
    private final HttpStatus httpStatus;
}
