package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.ApiResponseEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Envoltorio estándar de toda respuesta del API.
 * Se construye siempre a partir de {@link ApiResponseEnum} para garantizar consistencia.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
    private final T data;

    public static <T> ApiResponse<T> of(ApiResponseEnum response, T data) {
        return ApiResponse.<T>builder()
                .code(response.getCode())
                .message(response.getDefaultMessage())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> of(ApiResponseEnum response, String mensajePersonalizado, T data) {
        return ApiResponse.<T>builder()
                .code(response.getCode())
                .message(mensajePersonalizado)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }
}
