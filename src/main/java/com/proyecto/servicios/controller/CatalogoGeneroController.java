package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoGeneroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de Género / Sexo", description = "Endpoints para la gestión del catálogo de géneros/sexo")
@RestController
public class CatalogoGeneroController {

    private final CatalogoGeneroService catalogoGeneroService;

    public CatalogoGeneroController(CatalogoGeneroService catalogoGeneroService) {
        this.catalogoGeneroService = catalogoGeneroService;
    }

    @Operation(summary = "Obtener género por ID")
    @GetMapping(value = "/catalogos/genero/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneros>> obtenerPorId(@PathVariable Long id) {
        CatalogoGeneros genero = catalogoGeneroService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @Operation(summary = "Actualizar género")
    @PutMapping(value = "/catalogos/genero/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneros>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoGeneros catalogoGeneros) {
        CatalogoGeneros genero = catalogoGeneroService.actualizar(id, catalogoGeneros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @Operation(summary = "Eliminar género")
    @DeleteMapping(value = "/catalogos/genero/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoGeneroService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todos los géneros")
    @GetMapping(value = "/catalogos/genero", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoGeneros>>> obtenerTodos() {
        List<CatalogoGeneros> generos = catalogoGeneroService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, generos));
    }

    @Operation(summary = "Crear nuevo género")
    @PostMapping(value = "/catalogos/genero", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneros>> crear(@Valid @RequestBody CatalogoGeneros catalogoGeneros) {
        CatalogoGeneros genero = catalogoGeneroService.crear(catalogoGeneros);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, genero), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar género por tipo")
    @GetMapping(value = "/catalogos/genero/buscar", params = "tipo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoGeneros>>> buscarPorTipo(@RequestParam String tipo) {
        List<CatalogoGeneros> generos = catalogoGeneroService.buscarPorTipo(tipo);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, generos));
    }
}