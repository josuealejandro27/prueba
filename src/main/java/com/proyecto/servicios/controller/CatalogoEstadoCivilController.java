package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoEstadoCivilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de Estado Civil", description = "Endpoints para la gestión del catálogo de estados civiles")
@RestController
public class CatalogoEstadoCivilController {

    private final CatalogoEstadoCivilService catalogoEstadoCivilService;

    public CatalogoEstadoCivilController(CatalogoEstadoCivilService catalogoEstadoCivilService) {
        this.catalogoEstadoCivilService = catalogoEstadoCivilService;
    }

    @Operation(summary = "Obtener estado civil por ID")
    @GetMapping(value = "/catalogos/estado-civil/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoCivil>> obtenerPorId(@PathVariable Long id) {
        CatalogoEstadoCivil estadoCivil = catalogoEstadoCivilService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estadoCivil));
    }

    @Operation(summary = "Actualizar estado civil")
    @PutMapping(value = "/catalogos/estado-civil/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoCivil>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoEstadoCivil catalogoEstadoCivil) {
        CatalogoEstadoCivil estadoCivil = catalogoEstadoCivilService.actualizar(id, catalogoEstadoCivil);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estadoCivil));
    }

    @Operation(summary = "Eliminar estado civil")
    @DeleteMapping(value = "/catalogos/estado-civil/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoEstadoCivilService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todos los estados civiles")
    @GetMapping(value = "/catalogos/estado-civil", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoEstadoCivil>>> obtenerTodos() {
        List<CatalogoEstadoCivil> estadosCiviles = catalogoEstadoCivilService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estadosCiviles));
    }

    @Operation(summary = "Crear nuevo estado civil")
    @PostMapping(value = "/catalogos/estado-civil", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoCivil>> crear(@Valid @RequestBody CatalogoEstadoCivil catalogoEstadoCivil) {
        CatalogoEstadoCivil estadoCivil = catalogoEstadoCivilService.crear(catalogoEstadoCivil);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, estadoCivil), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar estado civil por nombre")
    @GetMapping(value = "/catalogos/estado-civil/buscar", params = "nombre", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoEstadoCivil>>> buscarPorNombre(@RequestParam String nombre) {
        List<CatalogoEstadoCivil> estadosCiviles = catalogoEstadoCivilService.buscarPorNombre(nombre);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estadosCiviles));
    }
}