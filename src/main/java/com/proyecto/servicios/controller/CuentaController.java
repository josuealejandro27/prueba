package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.CuentaUpdateRequest;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Gestión de Cuentas Bancarias", description = "Endpoints para la apertura, consulta de saldo, filtrado y administración de cuentas bancarias")
@RestController
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    // Consultar cuenta por número
    @Operation(summary = "Consultar cuenta por número")
    @GetMapping(value = "/cuentas/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuentaPorNumero(@PathVariable String numeroCuenta) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta));
    }

    // Consultar cuentas por cliente
    @Operation(summary = "Consultar cuentas con filtros")
    @GetMapping(value = "/cuentas", params = "clienteId", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasPorCliente(@RequestParam Long clienteId) {
        List<CuentaResponse> cuentas = cuentaService.obtenerCuentasPorCliente(clienteId);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuentas));
    }

    // Consultar saldo de una cuenta
    @Operation(summary = "Consultar saldo de cuenta")
    @GetMapping(value = "/cuentas/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> consultarSaldo(@PathVariable String numeroCuenta) {
        CuentaResponse cuenta = cuentaService.obtenerCuentaPorNumero(numeroCuenta);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta.getSaldo()));
    }

    // Consultar cuentas por estatus
    @Operation(summary = "Consultar cuentas con filtros")
    @GetMapping(value = "/cuentas", params = "estatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasPorEstatus(@RequestParam String estatus) {
        List<CuentaResponse> cuentas = cuentaService.obtenerCuentasPorEstatus(estatus);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuentas));
    }

    // Consultar cuentas activas
    @Operation(summary = "Consultar cuentas con filtros")
    @GetMapping(value = "/cuentas", params = "activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasActivas(@RequestParam boolean activas) {
        List<CuentaResponse> cuentas = cuentaService.obtenerCuentasActivas();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuentas));
    }

    // Crear cuenta
    @Operation(summary = "Crear nueva cuenta bancaria")
    @PostMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> crearCuenta(@RequestParam Long clienteId, @RequestParam(required = false) String numeroCuenta) {
        CuentaResponse cuenta = cuentaService.crearCuenta(clienteId, numeroCuenta);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, cuenta), HttpStatus.CREATED);
    }

    // Actualizar cuenta (parcial) - PUT según requisito académico
    @Operation(summary = "Actualizar estatus de cuenta")
    @PutMapping(value = "/cuentas/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CuentaResponse>> actualizarCuenta(@PathVariable String numeroCuenta, @Valid @RequestBody CuentaUpdateRequest request) {
        CuentaResponse cuenta = cuentaService.actualizarCuenta(numeroCuenta, request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cuenta));
    }
}
