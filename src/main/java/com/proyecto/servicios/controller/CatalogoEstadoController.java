package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoSearchRequest;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoEstadoService;
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

@Tag(name = "Catálogo de Estado", description = "Endpoints para la gestión del catálogo de estados / entidades federativas")
@RestController
public class CatalogoEstadoController {

    private final CatalogoEstadoService catalogoEstadoService;

    public CatalogoEstadoController(CatalogoEstadoService catalogoEstadoService) {
        this.catalogoEstadoService = catalogoEstadoService;
    }

    @Operation(summary = "Consultar estado por ID")
    @GetMapping(value = "/catalogos/estado/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoResponse>> obtenerPorId(
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoEstadoResponse estado = catalogoEstadoService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estado));
    }

    @Operation(summary = "Buscar estados con filtros",
            description = "Todos los criterios son opcionales y combinables. Sin criterios devuelve todos los estados. "
                    + "'nombre' aplica coincidencia parcial sin distinguir mayúsculas.")
    @GetMapping(value = "/catalogos/estado/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoEstadoResponse>>> buscar(
            @Valid @RequestBody(required = false) CatalogoEstadoSearchRequest request) {
        CatalogoEstadoSearchRequest filtros = request != null ? request : new CatalogoEstadoSearchRequest();
        Page<CatalogoEstadoResponse> pagina = catalogoEstadoService.buscar(filtros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(pagina)));
    }

    @Operation(summary = "Crear estado")
    @PostMapping(value = "/catalogos/estado/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoResponse>> crear(
            @Valid @RequestBody CatalogoEstadoRequest request) {
        CatalogoEstadoResponse estado = catalogoEstadoService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, estado), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar estado")
    @PutMapping(value = "/catalogos/estado/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoResponse>> actualizar(
            @Valid @RequestBody CatalogoEstadoActualizaRequest request) {
        CatalogoEstadoResponse estado = catalogoEstadoService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estado));
    }

    @Operation(summary = "Eliminar estado")
    @PatchMapping(value = "/catalogos/estado/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoEstadoService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
