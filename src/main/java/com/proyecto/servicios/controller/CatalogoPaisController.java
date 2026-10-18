package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisResponse;
import com.proyecto.servicios.model.catalogo.CatalogoPaisSearchRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoPaisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo de País", description = "Endpoints para la gestión del catálogo de países")
@RestController
public class CatalogoPaisController {

    private final CatalogoPaisService catalogoPaisService;

    public CatalogoPaisController(CatalogoPaisService catalogoPaisService) {
        this.catalogoPaisService = catalogoPaisService;
    }

    @Operation(summary = "Consultar país por ID")
    @GetMapping(value = "/catalogos/pais/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoPaisResponse>> obtenerPorId(
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoPaisResponse pais = catalogoPaisService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, pais));
    }

    @Operation(summary = "Consultar países con filtros",
            description = "Todos los criterios son opcionales y combinables y se envían como parámetros de consulta "
                    + "(query params): ?nombre=...&pagina=0&tamano=10. Sin parámetros devuelve todos los países. "
                    + "'nombre' aplica coincidencia parcial sin distinguir mayúsculas. "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'.")
    @GetMapping(value = "/catalogos/pais/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoPaisResponse>>> buscar(
            @Valid @ModelAttribute CatalogoPaisSearchRequest request) {
        Page<CatalogoPaisResponse> paises = catalogoPaisService.buscar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(paises)));
    }

    @Operation(summary = "Crear país")
    @PostMapping(value = "/catalogos/pais/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoPaisResponse>> crear(
            @Valid @RequestBody CatalogoPaisRequest request) {
        CatalogoPaisResponse pais = catalogoPaisService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, pais), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar país")
    @PutMapping(value = "/catalogos/pais/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoPaisResponse>> actualizar(
            @Valid @RequestBody CatalogoPaisActualizaRequest request) {
        CatalogoPaisResponse pais = catalogoPaisService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, pais));
    }

    @Operation(summary = "Eliminar país")
    @PatchMapping(value = "/catalogos/pais/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoPaisService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
