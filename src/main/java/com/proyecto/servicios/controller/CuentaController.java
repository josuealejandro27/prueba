package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.CuentaCreateRequest;
import com.proyecto.servicios.model.clientes.CuentaNumeroRequest;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.CuentaSearchRequest;
import com.proyecto.servicios.model.clientes.CuentaUpdateRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Gestión de Cuentas Bancarias", description = "Endpoints para la apertura, consulta por número de cuenta, filtrado y administración de cuentas bancarias")
@RestController
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    // Consultar cuenta por número
    @Operation(summary = "Consultar cuenta por número",
            description = "Método POST con el número de cuenta enviado en el cuerpo (DTO) para respetar la "
                    + "restricción de no usar @PathVariable ni @RequestParam.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Número de cuenta: {\"numeroCuenta\":\"...\"}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CuentaNumeroRequest.class))))
    @PostMapping(value = "/cuentas/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorNumero(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody CuentaNumeroRequest request) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta));
    }

    @Operation(summary = "Consultar cuentas con filtros (POST)",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"clienteId\":1,\"activas\":true,\"estatus\":\"ACTIVA\",\"pagina\":0,\"tamano\":10}. "
                    + "Sin cuerpo (o cuerpo vacío) devuelve todas las cuentas paginadas. "
                    + "'clienteId' filtra las cuentas de un cliente; 'activas': true = solo cuentas ACTIVAS, "
                    + "false = solo no activas; 'estatus' filtra por estatus (ACTIVA/BLOQUEADA/INACTIVA/CANCELADA). "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta incluye "
                    + "'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"clienteId\":1,\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CuentaSearchRequest.class))))
    @PostMapping(value = "/cuentas/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CuentaResponse>>> buscarCuentas(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody(required = false) CuentaSearchRequest request) {
        CuentaSearchRequest filtro = request != null ? request : new CuentaSearchRequest();
        Page<CuentaResponse> cuentas = cuentaService.buscarCuentas(filtro);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(cuentas)));
    }

    // Crear cuenta
    @Operation(summary = "Crear nueva cuenta bancaria")
    @PostMapping(value = "/cuentas/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> crearCuenta(
            @Valid @RequestBody CuentaCreateRequest request) {
        CuentaResponse cuenta = cuentaService.crearCuenta(request.getClienteId(), request.getNumeroCuenta());
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, cuenta), HttpStatus.CREATED);
    }

    // Actualizar cuenta (número, saldo o estatus) - POST según requisito académico
    @Operation(summary = "Actualizar cuenta (identificador por número de cuenta)")
    @PostMapping(value = "/cuentas/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> actualizarCuenta(
            @Valid @RequestBody CuentaUpdateRequest request) {
        CuentaResponse cuenta = cuentaService.actualizarCuenta(request.getNumeroCuenta(), request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta));
    }
}
