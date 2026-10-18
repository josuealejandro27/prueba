package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadResponse;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadSearchRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoNacionalidadService;
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

@Tag(name = "Catálogo de Nacionalidad", description = "Endpoints para la gestión del catálogo de nacionalidades")
@RestController
public class CatalogoNacionalidadController {

    private final CatalogoNacionalidadService catalogoNacionalidadService;

    public CatalogoNacionalidadController(CatalogoNacionalidadService catalogoNacionalidadService) {
        this.catalogoNacionalidadService = catalogoNacionalidadService;
    }

    @Operation(summary = "Consultar nacionalidad por ID")
    @GetMapping(value = "/catalogos/nacionalidad/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoNacionalidadResponse>> obtenerPorId(
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoNacionalidadResponse nacionalidad = catalogoNacionalidadService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, nacionalidad));
    }

    @Operation(summary = "Consultar nacionalidades con filtros",
            description = "Todos los criterios son opcionales y combinables. Sin criterios devuelve todas las nacionalidades.")
    @GetMapping(value = "/catalogos/nacionalidad/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoNacionalidadResponse>>> buscar(
            @Valid @RequestBody(required = false) CatalogoNacionalidadSearchRequest request) {
        CatalogoNacionalidadSearchRequest filtros = request != null ? request : new CatalogoNacionalidadSearchRequest();
        Page<CatalogoNacionalidadResponse> nacionalidades = catalogoNacionalidadService.buscar(filtros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(nacionalidades)));
    }

    @Operation(summary = "Crear nacionalidad")
    @PostMapping(value = "/catalogos/nacionalidad/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoNacionalidadResponse>> crear(
            @Valid @RequestBody CatalogoNacionalidadRequest request) {
        CatalogoNacionalidadResponse nacionalidad = catalogoNacionalidadService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, nacionalidad), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar nacionalidad")
    @PutMapping(value = "/catalogos/nacionalidad/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoNacionalidadResponse>> actualizar(
            @Valid @RequestBody CatalogoNacionalidadActualizaRequest request) {
        CatalogoNacionalidadResponse nacionalidad = catalogoNacionalidadService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, nacionalidad));
    }

    @Operation(summary = "Eliminar nacionalidad")
    @PatchMapping(value = "/catalogos/nacionalidad/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoNacionalidadService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
