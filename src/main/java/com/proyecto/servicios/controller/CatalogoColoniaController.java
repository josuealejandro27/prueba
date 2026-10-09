package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoColonia;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoColoniaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de Colonia", description = "Endpoints para la gestión del catálogo de colonias")
@RestController
public class CatalogoColoniaController {

    private final CatalogoColoniaService catalogoColoniaService;

    public CatalogoColoniaController(CatalogoColoniaService catalogoColoniaService) {
        this.catalogoColoniaService = catalogoColoniaService;
    }

    @Operation(summary = "Obtener colonia por ID")
    @GetMapping(value = "/catalogos/colonia/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoColonia>> obtenerPorId(@PathVariable Long id) {
        CatalogoColonia colonia = catalogoColoniaService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, colonia));
    }

    @Operation(summary = "Actualizar colonia")
    @PutMapping(value = "/catalogos/colonia/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoColonia>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoColonia catalogoColonia) {
        CatalogoColonia colonia = catalogoColoniaService.actualizar(id, catalogoColonia);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, colonia));
    }

    @Operation(summary = "Eliminar colonia")
    @DeleteMapping(value = "/catalogos/colonia/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoColoniaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todas las colonias")
    @GetMapping(value = "/catalogos/colonia", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoColonia>>> obtenerTodos() {
        List<CatalogoColonia> colonias = catalogoColoniaService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, colonias));
    }

    @Operation(summary = "Crear nueva colonia")
    @PostMapping(value = "/catalogos/colonia", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoColonia>> crear(@Valid @RequestBody CatalogoColonia catalogoColonia) {
        CatalogoColonia colonia = catalogoColoniaService.crear(catalogoColonia);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, colonia), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar colonias por ID de municipio")
    @GetMapping(value = "/catalogos/colonia/municipio/{municipioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoColonia>>> obtenerPorMunicipio(@PathVariable Long municipioId) {
        List<CatalogoColonia> colonias = catalogoColoniaService.obtenerPorMunicipioId(municipioId);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, colonias));
    }
}