package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.CatalogoGeneroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CatalogoGeneroController {

    private final CatalogoGeneroService catalogoGeneroService;

    public CatalogoGeneroController(CatalogoGeneroService catalogoGeneroService) {
        this.catalogoGeneroService = catalogoGeneroService;
    }

    @GetMapping(value = "/catalogo/generos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<CatalogoGeneros>>> obtenerTodos() {
        List<CatalogoGeneros> generos = catalogoGeneroService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, generos));
    }

    @GetMapping(value = "/catalogo/generos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneros>> obtenerPorId(@PathVariable Long id) {
        CatalogoGeneros genero = catalogoGeneroService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @PostMapping(value = "/catalogo/generos", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneros>> crear(@RequestBody CatalogoGeneros catalogoGeneros) {
        CatalogoGeneros genero = catalogoGeneroService.crear(catalogoGeneros);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.OK, genero), HttpStatus.CREATED);
    }

    @PutMapping(value = "/catalogo/generos/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneros>> actualizar(@PathVariable Long id, @RequestBody CatalogoGeneros catalogoGeneros) {
        CatalogoGeneros genero = catalogoGeneroService.actualizar(id, catalogoGeneros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @DeleteMapping(value = "/catalogo/generos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        catalogoGeneroService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
