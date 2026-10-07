package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

    @ExceptionHandler(ClienteException.class)
    public ResponseEntity<ApiResponse<Void>> manejarClienteException(ClienteException ex) {
        log.error("ClienteException [{}] - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(CuentaException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuentaException(CuentaException ex) {
        log.error("CuentaException [{}] - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(UsuarioException.class)
    public ResponseEntity<ApiResponse<Void>> manejarUsuarioException(UsuarioException ex) {
        log.error("UsuarioException [{}] - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> manejarValidationException(ValidationException ex) {
        log.error("ValidationException [{}] - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.error("Errores de validación: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .code("VALIDATION_ERROR")
                        .message("Error de validación en los campos")
                        .timestamp(LocalDateTime.now())
                        .data(errores)
                        .build());
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

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAccesoDenegado(org.springframework.security.access.AccessDeniedException ex) {
        log.error("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .code("FORBIDDEN")
                        .message("Acceso denegado")
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCredencialesInvalidas(org.springframework.security.authentication.BadCredentialsException ex) {
        log.error("Credenciales inválidas: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .code("UNAUTHORIZED")
                        .message("Credenciales inválidas")
                        .timestamp(LocalDateTime.now())
                        .data(null)
                        .build());
    }

    private ResponseEntity<ApiResponse<Void>> responder(ApiResponseEnum response) {
        return ResponseEntity.status(response.getHttpStatus())
                .body(ApiResponse.of(response, null));
    }
}
