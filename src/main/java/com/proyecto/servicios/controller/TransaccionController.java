package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.service.PagoFinancieroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Transacciones Financieras", description = "Endpoints para procesamiento de pagos y recargas con Gestopago")
@RestController
public class TransaccionController {

    private final PagoFinancieroService pagoService;

    public TransaccionController(PagoFinancieroService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Procesar pago de servicios o recarga electrónica",
            description = "Ejecuta el débito en dos fases con bloqueo pesimista contra doble gasto, "
                    + "firma criptográfica AES y liquidación ante el proveedor Gestopago (sendTx.do). "
                    + "Todos los parámetros se reciben obligatoriamente en el cuerpo (@RequestBody).")
    @PostMapping(value = "/transacciones/pagar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoResponse>> pagar(@Valid @RequestBody PagoRequest request) {
        PagoResponse response = pagoService.procesarPago(request);

        if (response.getEstado() == EstadoTransaccion.EXITOSA) {
            return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, response));
        }

        if (response.getEstado() == EstadoTransaccion.TIMEOUT) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(ApiResponse.<PagoResponse>builder()
                            .code("TIMEOUT")
                            .message("La transacción se encuentra en custodia pendiente de confirmación por el proveedor")
                            .timestamp(LocalDateTime.now())
                            .data(response)
                            .build());
        }

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.<PagoResponse>builder()
                        .code("TRANSACCION_RECHAZADA")
                        .message(response.getMensajeProveedor() != null ? response.getMensajeProveedor() : "Transacción rechazada por el proveedor")
                        .timestamp(LocalDateTime.now())
                        .data(response)
                        .build());
    }

    @Operation(summary = "Consultar estado de transacción por folio o clave de idempotencia",
            description = "Consulta la transacción financiera enviando el folio o clave en el cuerpo (@RequestBody).")
    @PostMapping(value = "/transacciones/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoResponse>> consultar(@Valid @RequestBody ConsultaTransaccionRequest request) {
        PagoResponse response = pagoService.consultarTransaccion(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, response));
    }
}

