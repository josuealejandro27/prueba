package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoMunicipioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catálogo de Municipio", description = "Endpoints para la gestión del catálogo de municipios")
@RestController
public class CatalogoMunicipioController {

    private final CatalogoMunicipioService catalogoMunicipioService;

    public CatalogoMunicipioController(CatalogoMunicipioService catalogoMunicipioService) {
        this.catalogoMunicipioService = catalogoMunicipioService;
    }

    @Operation(summary = "Obtener municipio por ID")
    @GetMapping(value = "/catalogos/municipio/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipio>> obtenerPorId(@PathVariable Long id) {
        CatalogoMunicipio municipio = catalogoMunicipioService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipio));
    }

    @Operation(summary = "Actualizar municipio")
    @PutMapping(value = "/catalogos/municipio/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipio>> actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoMunicipio catalogoMunicipio) {
        CatalogoMunicipio municipio = catalogoMunicipioService.actualizar(id, catalogoMunicipio);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipio));
    }

    @Operation(summary = "Eliminar municipio")
    @DeleteMapping(value = "/catalogos/municipio/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoMunicipioService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }

    @Operation(summary = "Obtener todos los municipios")
    @GetMapping(value = "/catalogos/municipio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoMunicipio>>> obtenerTodos() {
        List<CatalogoMunicipio> municipios = catalogoMunicipioService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipios));
    }

    @Operation(summary = "Crear nuevo municipio")
    @PostMapping(value = "/catalogos/municipio", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipio>> crear(@Valid @RequestBody CatalogoMunicipio catalogoMunicipio) {
        CatalogoMunicipio municipio = catalogoMunicipioService.crear(catalogoMunicipio);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, municipio), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar municipios por ID de estado")
    @GetMapping(value = "/catalogos/municipio/estado/{estadoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoMunicipio>>> obtenerPorEstado(@PathVariable Long estadoId) {
        List<CatalogoMunicipio> municipios = catalogoMunicipioService.obtenerPorEstadoId(estadoId);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipios));
    }
}