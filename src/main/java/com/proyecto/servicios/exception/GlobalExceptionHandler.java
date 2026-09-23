package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Manejador global de excepciones.
 * Todas las respuestas de error se estandarizan mediante {@link ApiResponse} + {@link ApiResponseEnum}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCatalogoException(CatalogoException ex) {
        ApiResponseEnum response = ex.getResponse();
        log.error("CatalogoException [{}] - {}", response.getCode(), ex.getMessage());
        return ResponseEntity.status(response.getHttpStatus())
                .body(ApiResponse.of(response, ex.getMessage(), null));
    }

    @ExceptionHandler(FeignException.Unauthorized.class)
    public ResponseEntity<ApiResponse<Void>> manejarNoAutorizado(FeignException.Unauthorized ex) {
        log.error("FeignException 401 del servicio externo: {}", ex.getMessage());
        return responder(ApiResponseEnum.ERROR_AUTENTICACION);
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ApiResponse<Void>> manejarProhibido(FeignException.Forbidden ex) {
        log.error("FeignException 403 del servicio externo: {}", ex.getMessage());
        return responder(ApiResponseEnum.ERROR_AUTENTICACION);
    }

    @ExceptionHandler({FeignException.class})
    public ResponseEntity<ApiResponse<Void>> manejarFeignException(FeignException ex) {
        log.error("FeignException del servicio externo: status={} - {}", ex.status(), ex.getMessage());
        return responder(ApiResponseEnum.ERROR_COMUNICACION);
    }

    @ExceptionHandler({TimeoutException.class, SocketTimeoutException.class})
    public ResponseEntity<ApiResponse<Void>> manejarTimeout(Exception ex) {
        log.error("Timeout consultando el catálogo: {}", ex.getMessage());
        return responder(ApiResponseEnum.TIMEOUT_SERVICIO);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCacheNoDisponible(DataAccessResourceFailureException ex) {
        log.error("Caché de MongoDB no accesible: {}", ex.getMessage());
        return responder(ApiResponseEnum.ERROR_COMUNICACION);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> manejarExceptionGenerica(Exception ex) {
        log.error("Error no controlado al consultar el catálogo: {}", ex.getMessage(), ex);
        return responder(ApiResponseEnum.ERROR_INTERNO);
    }

    private ResponseEntity<ApiResponse<Void>> responder(ApiResponseEnum response) {
        return ResponseEntity.status(response.getHttpStatus())
                .body(ApiResponse.of(response, null));
    }
}
