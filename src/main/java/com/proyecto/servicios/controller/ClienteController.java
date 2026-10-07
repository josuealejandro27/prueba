package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.*;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // Crear cliente
    @PostMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> crearCliente(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse cliente = clienteService.crearCliente(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, cliente), HttpStatus.CREATED);
    }

    // Actualizar cliente (parcial)
    @PatchMapping(value = "/clientes/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Consultar todos los clientes
    @GetMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerTodos() {
        List<ClienteResponse> clientes = clienteService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    // Consultar cliente por ID
    @GetMapping(value = "/clientes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerCliente(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.obtenerCliente(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Buscar cliente por CURP
    @GetMapping(value = "/clientes", params = "curp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorCurp(@RequestParam String curp) {
        ClienteResponse cliente = clienteService.obtenerClientePorCurp(curp);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Buscar cliente por RFC
    @GetMapping(value = "/clientes", params = "rfc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorRfc(@RequestParam String rfc) {
        ClienteResponse cliente = clienteService.obtenerClientePorRfc(rfc);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Buscar cliente por correo
    @GetMapping(value = "/clientes", params = "correo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorCorreo(@RequestParam String correo) {
        ClienteResponse cliente = clienteService.obtenerClientePorCorreo(correo);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Buscar cliente por número de cuenta
    @GetMapping(value = "/clientes", params = "numeroCuenta", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerClientePorNumeroCuenta(@RequestParam String numeroCuenta) {
        ClienteResponse cliente = clienteService.obtenerClientePorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Buscar clientes por nombre
    @GetMapping(value = "/clientes", params = "nombre", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> buscarPorNombre(@RequestParam String nombre) {
        List<ClienteResponse> clientes = clienteService.buscarPorNombre(nombre);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    // Buscar clientes por apellido paterno
    @GetMapping(value = "/clientes", params = "apellidoPaterno", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> buscarPorApellidoPaterno(@RequestParam String apellidoPaterno) {
        List<ClienteResponse> clientes = clienteService.buscarPorApellidoPaterno(apellidoPaterno);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    // Buscar clientes por apellido materno
    @GetMapping(value = "/clientes", params = "apellidoMaterno", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> buscarPorApellidoMaterno(@RequestParam String apellidoMaterno) {
        List<ClienteResponse> clientes = clienteService.buscarPorApellidoMaterno(apellidoMaterno);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    // Consultar clientes activos
    @GetMapping(value = "/clientes", params = "activos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesActivos(@RequestParam boolean activos) {
        List<ClienteResponse> clientes = clienteService.obtenerClientesActivos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    // Consultar clientes por rango de fechas
    @GetMapping(value = "/clientes", params = {"fechaInicio", "fechaFin"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerClientesPorRangoFechas(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaFin) {
        List<ClienteResponse> clientes = clienteService.obtenerClientesPorRangoFechas(fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    // Bloquear/desbloquear cuenta
    @PatchMapping(value = "/clientes/{id}/bloqueo-cuenta", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> bloquearCuenta(@PathVariable Long id, @Valid @RequestBody BloqueoRequest request) {
        ClienteResponse cliente = clienteService.bloquearCuenta(id, request.isBloqueado());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Bloquear/desbloquear login
    @PatchMapping(value = "/clientes/{id}/bloqueo-login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> bloquearLogin(@PathVariable Long id, @Valid @RequestBody BloqueoRequest request) {
        ClienteResponse cliente = clienteService.bloquearLogin(id, request.isBloqueado());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Desactivar cliente (baja lógica)
    @PatchMapping(value = "/clientes/{id}/desactivar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> desactivarCliente(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.desactivarCliente(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    // Activar cliente
    @PatchMapping(value = "/clientes/{id}/activar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> activarCliente(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.activarCliente(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }
}
