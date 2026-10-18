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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Gestión de Cuentas Bancarias", description = "Endpoints para la apertura, consulta de saldo, filtrado y administración de cuentas bancarias")
@RestController
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    // Consultar cuenta por número
    @Operation(summary = "Consultar cuenta por número",
            description = "Método GET con el número de cuenta enviado en el cuerpo (DTO) para respetar la "
                    + "restricción de no usar @PathVariable ni @RequestParam.")
    @GetMapping(value = "/cuentas/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorNumero(
            @Valid @RequestBody CuentaNumeroRequest request) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta));
    }

    @Operation(summary = "Consultar cuentas con filtros (GET)",
            description = "Todos los criterios son opcionales y combinables y se envían como parámetros de consulta "
                    + "(query params): ?clienteId=...&activas=true&estatus=ACTIVA&pagina=0&tamano=10. "
                    + "'clienteId' filtra las cuentas de un cliente; 'activas': true = solo cuentas ACTIVAS, "
                    + "false = solo no activas; 'estatus' filtra por estatus (ACTIVA/BLOQUEADA/INACTIVA/CANCELADA). "
                    + "Sin parámetros devuelve todas las cuentas paginadas.")
    @GetMapping(value = "/cuentas/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CuentaResponse>>> buscarCuentas(
            @Valid @ModelAttribute CuentaSearchRequest request) {
        Page<CuentaResponse> cuentas = cuentaService.buscarCuentas(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(cuentas)));
    }

    // Consultar saldo de una cuenta
    @Operation(summary = "Consultar saldo de cuenta",
            description = "Método GET con el número de cuenta enviado en el cuerpo (DTO).")
    @GetMapping(value = "/cuentas/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<BigDecimal>> consultarSaldo(
            @Valid @RequestBody CuentaNumeroRequest request) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta.getSaldo()));
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

    // Actualizar cuenta (número, saldo o estatus) - PUT según requisito académico
    @Operation(summary = "Actualizar cuenta (identificador por número de cuenta)")
    @PutMapping(value = "/cuentas/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> actualizarCuenta(
            @Valid @RequestBody CuentaUpdateRequest request) {
        CuentaResponse cuenta = cuentaService.actualizarCuenta(request.getNumeroCuenta(), request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta));
    }
}
