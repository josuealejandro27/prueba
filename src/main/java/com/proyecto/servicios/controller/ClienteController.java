package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.BloqueoRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> crearCliente(@RequestBody ClienteRequest request) {
        ClienteResponse cliente = clienteService.crearCliente(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, cliente), HttpStatus.CREATED);
    }

    @PutMapping(value = "/clientes/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(@PathVariable Long id, @RequestBody ClienteRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @GetMapping(value = "/clientes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerCliente(@PathVariable Long id) {
        ClienteResponse cliente = clienteService.obtenerCliente(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @GetMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> obtenerTodos() {
        List<ClienteResponse> clientes = clienteService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, clientes));
    }

    @PatchMapping(value = "/clientes/{id}/bloqueo-cuenta", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> bloquearCuenta(@PathVariable Long id, @RequestBody BloqueoRequest request) {
        ClienteResponse cliente = clienteService.bloquearCuenta(id, request.isBloqueado());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @PatchMapping(value = "/clientes/{id}/bloqueo-login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> bloquearLogin(@PathVariable Long id, @RequestBody BloqueoRequest request) {
        ClienteResponse cliente = clienteService.bloquearLogin(id, request.isBloqueado());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }
}
