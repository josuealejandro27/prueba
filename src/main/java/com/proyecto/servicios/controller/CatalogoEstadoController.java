package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoEstadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de Estado", description = "Endpoints para la gestión del catálogo de estados / entidades federativas")
@RestController
public class CatalogoEstadoController {

    private final CatalogoEstadoService catalogoEstadoService;

    public CatalogoEstadoController(CatalogoEstadoService catalogoEstadoService) {
        this.catalogoEstadoService = catalogoEstadoService;
    }

    @Operation(summary = "Obtener estado por ID")
    @GetMapping(value = "/catalogos/estado/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstado>> obtenerPorId(@PathVariable Long id) {
        CatalogoEstado estado = catalogoEstadoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estado));
    }

    @Operation(summary = "Actualizar estado")
    @PutMapping(value = "/catalogos/estado/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstado>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoEstado catalogoEstado) {
        CatalogoEstado estado = catalogoEstadoService.actualizar(id, catalogoEstado);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estado));
    }

    @Operation(summary = "Eliminar estado")
    @DeleteMapping(value = "/catalogos/estado/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoEstadoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todos los estados")
    @GetMapping(value = "/catalogos/estado", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoEstado>>> obtenerTodos() {
        List<CatalogoEstado> estados = catalogoEstadoService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estados));
    }

    @Operation(summary = "Crear nuevo estado")
    @PostMapping(value = "/catalogos/estado", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstado>> crear(@Valid @RequestBody CatalogoEstado catalogoEstado) {
        CatalogoEstado estado = catalogoEstadoService.crear(catalogoEstado);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, estado), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar estados por ID de país")
    @GetMapping(value = "/catalogos/estado/pais/{paisId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoEstado>>> obtenerPorPais(@PathVariable Long paisId) {
        List<CatalogoEstado> estados = catalogoEstadoService.obtenerPorPaisId(paisId);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estados));
    }
}