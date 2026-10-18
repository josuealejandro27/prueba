package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroRequest;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroResponse;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroSearchRequest;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoGeneroService;
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

@Tag(name = "Catálogo de Género / Sexo", description = "Endpoints para la gestión del catálogo de géneros/sexo")
@RestController
public class CatalogoGeneroController {

    private final CatalogoGeneroService catalogoGeneroService;

    public CatalogoGeneroController(CatalogoGeneroService catalogoGeneroService) {
        this.catalogoGeneroService = catalogoGeneroService;
    }

    @Operation(summary = "Consultar género por ID")
    @GetMapping(value = "/catalogos/genero/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneroResponse>> obtenerPorId(
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoGeneroResponse genero = catalogoGeneroService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @Operation(summary = "Consultar géneros con filtros",
            description = "Todos los criterios son opcionales y combinables y se envían como parámetros de consulta "
                    + "(query params): ?tipo=...&pagina=0&tamano=10. Sin parámetros devuelve todos los géneros. "
                    + "'tipo' aplica coincidencia parcial sin distinguir mayúsculas. "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'.")
    @GetMapping(value = "/catalogos/genero/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoGeneroResponse>>> buscar(
            @Valid @ModelAttribute CatalogoGeneroSearchRequest request) {
        Page<CatalogoGeneroResponse> generos = catalogoGeneroService.buscar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(generos)));
    }

    @Operation(summary = "Crear género")
    @PostMapping(value = "/catalogos/genero/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneroResponse>> crear(
            @Valid @RequestBody CatalogoGeneroRequest request) {
        CatalogoGeneroResponse genero = catalogoGeneroService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, genero), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar género")
    @PutMapping(value = "/catalogos/genero/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneroResponse>> actualizar(
            @Valid @RequestBody CatalogoGeneroActualizaRequest request) {
        CatalogoGeneroResponse genero = catalogoGeneroService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @Operation(summary = "Eliminar género")
    @PatchMapping(value = "/catalogos/genero/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoGeneroService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
