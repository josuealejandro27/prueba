package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.service.PagoFinancieroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Transacciones Financieras", description = "Endpoints para procesamiento seguro de pagos de servicios y recargas electrónicas con Gestopago")
@RestController
@SecurityRequirement(name = "BearerAuth")
public class TransaccionController {

    private final PagoFinancieroService pagoService;

    public TransaccionController(PagoFinancieroService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(
            summary = "Procesar pago de servicios o recarga electrónica",
            description = "Ejecuta un débito financiero en dos fases con bloqueo pesimista contra doble gasto, "
                    + "firma criptográfica AES-256/CBC y liquidación ante el proveedor externo Gestopago (sendTx.do). "
                    + "Todos los parámetros se reciben obligatoriamente en el cuerpo (@RequestBody en JSON)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transacción procesada exitosamente por Gestopago",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Pago Exitoso",
                                    value = """
                                            {
                                              "code": "200",
                                              "message": "Operación exitosa",
                                              "timestamp": "2026-10-28T22:30:00",
                                              "data": {
                                                "folio": "c56a4180-65aa-42ec-a945-5fd21dec0538",
                                                "idempotencyKey": "tx-cli-20261028-001",
                                                "numeroCuenta": "1234567890123456",
                                                "monto": 99.90,
                                                "estado": "EXITOSA",
                                                "codigoRespuestaProveedor": "00",
                                                "mensajeProveedor": "TRANSACCION EXITOSA",
                                                "folioProveedor": "GP-987654",
                                                "saldoRestante": 400.10,
                                                "fechaTransaccion": "2026-10-28T22:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Error de validación en los datos de entrada (Bean Validation)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Datos Inválidos",
                                    value = """
                                            {
                                              "code": "VALIDATION_ERROR",
                                              "message": "El número de cuenta debe tener exactamente 16 caracteres; El monto a pagar debe ser mayor a 0",
                                              "timestamp": "2026-10-28T22:30:00",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado, token JWT inválido o PIN de seguridad incorrecto",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "PIN Inválido",
                                    value = """
                                            {
                                              "code": "PIN_INVALIDO",
                                              "message": "El PIN de seguridad proporcionado es incorrecto",
                                              "timestamp": "2026-10-28T22:30:00",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Transacción rechazada (saldo insuficiente, cuenta inactiva o rechazo por el proveedor externo)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Rechazo del Proveedor",
                                    value = """
                                            {
                                              "code": "TRANSACCION_RECHAZADA",
                                              "message": "TELEFONO O CUENTA DESTINO INVALIDA",
                                              "timestamp": "2026-10-28T22:30:00",
                                              "data": {
                                                "folio": "c56a4180-65aa-42ec-a945-5fd21dec0538",
                                                "idempotencyKey": "tx-cli-20261028-002",
                                                "numeroCuenta": "1234567890123456",
                                                "monto": 99.90,
                                                "estado": "RECHAZADA",
                                                "codigoRespuestaProveedor": "80",
                                                "mensajeProveedor": "TELEFONO O CUENTA DESTINO INVALIDA",
                                                "folioProveedor": null,
                                                "saldoRestante": null,
                                                "fechaTransaccion": "2026-10-28T22:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "Timeout al comunicar con Gestopago; la transacción queda en custodia pendiente de confirmación",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Timeout",
                                    value = """
                                            {
                                              "code": "TIMEOUT",
                                              "message": "La transacción se encuentra en custodia pendiente de confirmación por el proveedor",
                                              "timestamp": "2026-10-28T22:30:00",
                                              "data": {
                                                "folio": "c56a4180-65aa-42ec-a945-5fd21dec0538",
                                                "idempotencyKey": "tx-cli-20261028-003",
                                                "numeroCuenta": "1234567890123456",
                                                "monto": 99.90,
                                                "estado": "TIMEOUT",
                                                "codigoRespuestaProveedor": "TIMEOUT",
                                                "mensajeProveedor": "Timeout de comunicación con Gestopago",
                                                "folioProveedor": null,
                                                "saldoRestante": null,
                                                "fechaTransaccion": "2026-10-28T22:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
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

    @Operation(
            summary = "Consultar estado de transacción por folio o clave de idempotencia",
            description = "Consulta la transacción financiera enviando el folio interno (UUID) o la clave de idempotencia en el cuerpo (@RequestBody)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transacción localizada exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Consulta Exitosa",
                                    value = """
                                            {
                                              "code": "200",
                                              "message": "Operación exitosa",
                                              "timestamp": "2026-10-28T22:30:00",
                                              "data": {
                                                "folio": "c56a4180-65aa-42ec-a945-5fd21dec0538",
                                                "idempotencyKey": "tx-cli-20261028-001",
                                                "numeroCuenta": "1234567890123456",
                                                "monto": 99.90,
                                                "estado": "EXITOSA",
                                                "codigoRespuestaProveedor": "00",
                                                "mensajeProveedor": "TRANSACCION EXITOSA",
                                                "folioProveedor": "GP-987654",
                                                "saldoRestante": 400.10,
                                                "fechaTransaccion": "2026-10-28T22:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Petición inválida (folio o clave ausente)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Transacción no encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping(value = "/transacciones/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoResponse>> consultar(@Valid @RequestBody ConsultaTransaccionRequest request) {
        PagoResponse response = pagoService.consultarTransaccion(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, response));
    }
}

