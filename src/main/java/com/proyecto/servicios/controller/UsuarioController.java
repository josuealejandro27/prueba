package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping(value = "/usuarios/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuario(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.obtenerUsuario(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @GetMapping(value = "/usuarios", params = "correo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuarioPorCorreo(@RequestParam String correo) {
        UsuarioResponse usuario = usuarioService.obtenerUsuarioPorCorreo(correo);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @GetMapping(value = "/usuarios", params = "clienteId", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuarioPorClienteId(@RequestParam Long clienteId) {
        UsuarioResponse usuario = usuarioService.obtenerUsuarioPorClienteId(clienteId);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @GetMapping(value = "/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> obtenerTodos() {
        List<UsuarioResponse> usuarios = usuarioService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuarios));
    }

    @GetMapping(value = "/usuarios", params = "activos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> obtenerUsuariosActivos(@RequestParam boolean activos) {
        List<UsuarioResponse> usuarios = usuarioService.obtenerUsuariosActivos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuarios));
    }

    @GetMapping(value = "/usuarios/filtro", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> buscarPorCorreo(@RequestParam String correo) {
        List<UsuarioResponse> usuarios = usuarioService.buscarPorCorreo(correo);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuarios));
    }

    @PostMapping(value = "/usuarios/agregar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(@RequestParam Long clienteId, @RequestParam String correo, @RequestParam String password) {
        UsuarioResponse usuario = usuarioService.crearUsuario(clienteId, correo, password);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, usuario), HttpStatus.CREATED);
    }

    @PutMapping(value = "/usuarios/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(@PathVariable Long id, @RequestParam(required = false) String correo, @RequestParam(required = false) String password) {
        UsuarioResponse usuario = usuarioService.actualizarUsuario(id, correo, password);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @PatchMapping(value = "/usuarios/{id}/desactivar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> desactivarUsuario(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }

    @PatchMapping(value = "/usuarios/{id}/activar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> activarUsuario(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.activarUsuario(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, usuario));
    }
}
