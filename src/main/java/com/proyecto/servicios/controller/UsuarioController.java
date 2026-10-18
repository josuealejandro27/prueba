package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioIdRequest;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.model.clientes.UsuarioUpdateRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.UsuarioService;
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

@Tag(name = "Gestión de Usuarios", description = "Endpoints para la administración de usuarios")
@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Consultar usuario por ID",
            description = "Método GET con el identificador enviado en el cuerpo (DTO) para respetar la "
                    + "restricción de no usar @PathVariable ni @RequestParam.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Identificador del usuario: {\"id\":1}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = UsuarioIdRequest.class))))
    @GetMapping(value = "/usuarios/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuario(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody UsuarioIdRequest request) {
        UsuarioResponse usuario = usuarioService.obtenerUsuario(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @Operation(summary = "Consultar usuarios con filtros y paginación",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"correo\":\"...\",\"clienteId\":1,\"activos\":true,\"pagina\":0,\"tamano\":10}. "
                    + "Sin cuerpo (o cuerpo vacío) devuelve todos los usuarios paginados. "
                    + "'correo' aplica coincidencia parcial sin distinguir mayúsculas; 'activos': true = solo activos, false = solo inactivos. "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta incluye "
                    + "'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"correo\":\"...\",\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = UsuarioSearchRequest.class))))
    @GetMapping(value = "/usuarios/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> buscarUsuarios(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody(required = false) UsuarioSearchRequest request) {
        UsuarioSearchRequest filtro = request != null ? request : new UsuarioSearchRequest();
        Page<UsuarioResponse> usuarios = usuarioService.buscarUsuarios(filtro);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(usuarios)));
    }

    @Operation(summary = "Crear usuario")
    @PostMapping(value = "/usuarios/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse usuario = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, usuario), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar usuario (parcial)")
    @PutMapping(value = "/usuarios/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(@Valid @RequestBody UsuarioUpdateRequest request) {
        UsuarioResponse usuario = usuarioService.actualizarUsuario(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @Operation(summary = "Desactivar usuario (baja lógica)")
    @PatchMapping(value = "/usuarios/desactivar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> desactivarUsuario(@Valid @RequestBody UsuarioIdRequest request) {
        UsuarioResponse usuario = usuarioService.desactivarUsuario(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @Operation(summary = "Activar usuario")
    @PatchMapping(value = "/usuarios/activar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> activarUsuario(@Valid @RequestBody UsuarioIdRequest request) {
        UsuarioResponse usuario = usuarioService.activarUsuario(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }
}
