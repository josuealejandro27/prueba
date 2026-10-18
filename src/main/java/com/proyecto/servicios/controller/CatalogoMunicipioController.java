package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioResponse;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioSearchRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoMunicipioService;
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

@Tag(name = "Catálogo de Municipio", description = "Endpoints para la gestión del catálogo de municipios")
@RestController
public class CatalogoMunicipioController {

    private final CatalogoMunicipioService catalogoMunicipioService;

    public CatalogoMunicipioController(CatalogoMunicipioService catalogoMunicipioService) {
        this.catalogoMunicipioService = catalogoMunicipioService;
    }

    @Operation(summary = "Consultar municipio por ID")
    @GetMapping(value = "/catalogos/municipio/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipioResponse>> obtenerPorId(
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoMunicipioResponse municipio = catalogoMunicipioService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipio));
    }

    @Operation(summary = "Buscar municipios con filtros",
            description = "Todos los criterios son opcionales y combinables. Sin criterios devuelve todos los municipios. "
                    + "'nombre' aplica coincidencia parcial sin distinguir mayúsculas.")
    @GetMapping(value = "/catalogos/municipio/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoMunicipioResponse>>> buscar(
            @Valid @RequestBody(required = false) CatalogoMunicipioSearchRequest request) {
        CatalogoMunicipioSearchRequest filtros = request != null ? request : new CatalogoMunicipioSearchRequest();
        Page<CatalogoMunicipioResponse> pagina = catalogoMunicipioService.buscar(filtros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(pagina)));
    }

    @Operation(summary = "Crear municipio")
    @PostMapping(value = "/catalogos/municipio/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipioResponse>> crear(
            @Valid @RequestBody CatalogoMunicipioRequest request) {
        CatalogoMunicipioResponse municipio = catalogoMunicipioService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, municipio), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar municipio")
    @PutMapping(value = "/catalogos/municipio/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipioResponse>> actualizar(
            @Valid @RequestBody CatalogoMunicipioActualizaRequest request) {
        CatalogoMunicipioResponse municipio = catalogoMunicipioService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipio));
    }

    @Operation(summary = "Eliminar municipio")
    @PatchMapping(value = "/catalogos/municipio/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoMunicipioService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
