package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaResponse;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaSearchRequest;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoColoniaService;
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

@Tag(name = "Catálogo de Colonia", description = "Endpoints para la gestión del catálogo de colonias")
@RestController
public class CatalogoColoniaController {

    private final CatalogoColoniaService catalogoColoniaService;

    public CatalogoColoniaController(CatalogoColoniaService catalogoColoniaService) {
        this.catalogoColoniaService = catalogoColoniaService;
    }

    @Operation(summary = "Consultar colonia por ID")
    @GetMapping(value = "/catalogos/colonia/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoColoniaResponse>> obtenerPorId(
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoColoniaResponse colonia = catalogoColoniaService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, colonia));
    }

    @Operation(summary = "Buscar colonias con filtros",
            description = "Todos los criterios son opcionales y combinables. Sin criterios devuelve todas las colonias. "
                    + "'nombre' aplica coincidencia parcial sin distinguir mayúsculas.")
    @GetMapping(value = "/catalogos/colonia/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoColoniaResponse>>> buscar(
            @Valid @RequestBody(required = false) CatalogoColoniaSearchRequest request) {
        CatalogoColoniaSearchRequest filtros = request != null ? request : new CatalogoColoniaSearchRequest();
        Page<CatalogoColoniaResponse> pagina = catalogoColoniaService.buscar(filtros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(pagina)));
    }

    @Operation(summary = "Crear colonia")
    @PostMapping(value = "/catalogos/colonia/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoColoniaResponse>> crear(
            @Valid @RequestBody CatalogoColoniaRequest request) {
        CatalogoColoniaResponse colonia = catalogoColoniaService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, colonia), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar colonia")
    @PutMapping(value = "/catalogos/colonia/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoColoniaResponse>> actualizar(
            @Valid @RequestBody CatalogoColoniaActualizaRequest request) {
        CatalogoColoniaResponse colonia = catalogoColoniaService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, colonia));
    }

    @Operation(summary = "Eliminar colonia")
    @PatchMapping(value = "/catalogos/colonia/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoColoniaService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
