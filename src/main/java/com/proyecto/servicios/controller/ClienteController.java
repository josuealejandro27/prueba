package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteBloqueoRequest;
import com.proyecto.servicios.model.clientes.ClienteIdRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.ClienteSearchRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Gestión de Clientes", description = "Endpoints para la gestión de clientes bancarios")
@RestController
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(summary = "Consultar cliente por ID",
            description = "Método GET con el identificador enviado en el cuerpo (DTO) para respetar la "
                    + "restricción de no usar @PathVariable ni @RequestParam.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Identificador del cliente: {\"id\":1}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = ClienteIdRequest.class))))
    @GetMapping(value = "/clientes/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerCliente(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody ClienteIdRequest request) {
        ClienteResponse cliente = clienteService.obtenerCliente(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @Operation(summary = "Buscar clientes con filtros (GET)",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"nombre\":\"...\",\"curp\":\"...\",\"pagina\":0,\"tamano\":10}. "
                    + "Sin cuerpo (o cuerpo vacío) devuelve todos los clientes paginados. "
                    + "Cubre las consultas por nombre, apellidos, CURP, RFC, correo, número de cuenta, "
                    + "estado activo y rango de fechas (fechaInicio/fechaFin con formato yyyy-MM-dd). "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta incluye "
                    + "'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"curp\":\"...\",\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = ClienteSearchRequest.class))))
    @GetMapping(value = "/clientes/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<ClienteResponse>>> buscarClientes(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody(required = false) ClienteSearchRequest request) {
        ClienteSearchRequest filtro = request != null ? request : new ClienteSearchRequest();
        Page<ClienteResponse> page = clienteService.buscarClientes(filtro);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(page)));
    }

    @Operation(summary = "Registrar cliente bancario")
    @PostMapping(value = "/clientes/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> crearCliente(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse cliente = clienteService.crearCliente(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, cliente), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar cliente (parcial)")
    @PutMapping(value = "/clientes/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(@Valid @RequestBody ClienteActualizaRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @Operation(summary = "Desactivar cliente (baja lógica)")
    @PatchMapping(value = "/clientes/desactivar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> desactivarCliente(@Valid @RequestBody ClienteIdRequest request) {
        ClienteResponse cliente = clienteService.desactivarCliente(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @Operation(summary = "Activar cliente")
    @PatchMapping(value = "/clientes/activar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> activarCliente(@Valid @RequestBody ClienteIdRequest request) {
        ClienteResponse cliente = clienteService.activarCliente(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @Operation(summary = "Bloquear/desbloquear cuenta")
    @PatchMapping(value = "/clientes/bloquear-cuenta", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> bloquearCuenta(@Valid @RequestBody ClienteBloqueoRequest request) {
        ClienteResponse cliente = clienteService.bloquearCuenta(request.getId(), request.getBloqueado());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }

    @Operation(summary = "Bloquear/desbloquear login")
    @PatchMapping(value = "/clientes/bloquear-login", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponse>> bloquearLogin(@Valid @RequestBody ClienteBloqueoRequest request) {
        ClienteResponse cliente = clienteService.bloquearLogin(request.getId(), request.getBloqueado());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, cliente));
    }
}
