package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoNacionalidad;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoNacionalidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de Nacionalidad", description = "Endpoints para la gestión del catálogo de nacionalidades")
@RestController
public class CatalogoNacionalidadController {

    private final CatalogoNacionalidadService catalogoNacionalidadService;

    public CatalogoNacionalidadController(CatalogoNacionalidadService catalogoNacionalidadService) {
        this.catalogoNacionalidadService = catalogoNacionalidadService;
    }

    @Operation(summary = "Obtener nacionalidad por ID")
    @GetMapping(value = "/catalogos/nacionalidad/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoNacionalidad>> obtenerPorId(@PathVariable Long id) {
        CatalogoNacionalidad nacionalidad = catalogoNacionalidadService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, nacionalidad));
    }

    @Operation(summary = "Actualizar nacionalidad")
    @PutMapping(value = "/catalogos/nacionalidad/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoNacionalidad>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoNacionalidad catalogoNacionalidad) {
        CatalogoNacionalidad nacionalidad = catalogoNacionalidadService.actualizar(id, catalogoNacionalidad);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, nacionalidad));
    }

    @Operation(summary = "Eliminar nacionalidad")
    @DeleteMapping(value = "/catalogos/nacionalidad/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoNacionalidadService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todas las nacionalidades")
    @GetMapping(value = "/catalogos/nacionalidad", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoNacionalidad>>> obtenerTodos() {
        List<CatalogoNacionalidad> nacionalidades = catalogoNacionalidadService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, nacionalidades));
    }

    @Operation(summary = "Crear nueva nacionalidad")
    @PostMapping(value = "/catalogos/nacionalidad", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoNacionalidad>> crear(@Valid @RequestBody CatalogoNacionalidad catalogoNacionalidad) {
        CatalogoNacionalidad nacionalidad = catalogoNacionalidadService.crear(catalogoNacionalidad);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, nacionalidad), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar nacionalidad por nombre")
    @GetMapping(value = "/catalogos/nacionalidad/buscar", params = "nombre", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoNacionalidad>>> buscarPorNombre(@RequestParam String nombre) {
        List<CatalogoNacionalidad> nacionalidades = catalogoNacionalidadService.buscarPorNombre(nombre);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, nacionalidades));
    }
}