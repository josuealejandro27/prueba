package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoPaisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de País", description = "Endpoints para la gestión del catálogo de países")
@RestController
public class CatalogoPaisController {

    private final CatalogoPaisService catalogoPaisService;

    public CatalogoPaisController(CatalogoPaisService catalogoPaisService) {
        this.catalogoPaisService = catalogoPaisService;
    }

    @Operation(summary = "Obtener país por ID")
    @GetMapping(value = "/catalogos/pais/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoPais>> obtenerPorId(@PathVariable Long id) {
        CatalogoPais pais = catalogoPaisService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, pais));
    }

    @Operation(summary = "Actualizar país")
    @PutMapping(value = "/catalogos/pais/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoPais>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoPais catalogoPais) {
        CatalogoPais pais = catalogoPaisService.actualizar(id, catalogoPais);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, pais));
    }

    @Operation(summary = "Eliminar país")
    @DeleteMapping(value = "/catalogos/pais/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoPaisService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todos los países")
    @GetMapping(value = "/catalogos/pais", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoPais>>> obtenerTodos() {
        List<CatalogoPais> paises = catalogoPaisService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, paises));
    }

    @Operation(summary = "Crear nuevo país")
    @PostMapping(value = "/catalogos/pais", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoPais>> crear(@Valid @RequestBody CatalogoPais catalogoPais) {
        CatalogoPais pais = catalogoPaisService.crear(catalogoPais);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, pais), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar país por nombre")
    @GetMapping(value = "/catalogos/pais/buscar", params = "nombre", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoPais>>> buscarPorNombre(@RequestParam String nombre) {
        List<CatalogoPais> paises = catalogoPaisService.buscarPorNombre(nombre);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, paises));
    }
}