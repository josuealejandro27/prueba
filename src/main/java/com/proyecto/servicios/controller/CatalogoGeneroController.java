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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo de Género / Sexo", description = "Endpoints para la gestión del catálogo de géneros/sexo")
@RestController
public class CatalogoGeneroController {

    private final CatalogoGeneroService catalogoGeneroService;

    public CatalogoGeneroController(CatalogoGeneroService catalogoGeneroService) {
        this.catalogoGeneroService = catalogoGeneroService;
    }

    @Operation(summary = "Consultar género por ID",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Identificador del registro: {\"id\":1}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CatalogoIdRequest.class))))
    @PostMapping(value = "/catalogos/genero/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneroResponse>> obtenerPorId(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoGeneroResponse genero = catalogoGeneroService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @Operation(summary = "Consultar géneros con filtros",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"tipo\":\"...\",\"pagina\":0,\"tamano\":10}. Sin cuerpo (o cuerpo vacío) devuelve "
                    + "todos los géneros paginados. 'tipo' aplica coincidencia parcial sin distinguir mayúsculas. "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta incluye "
                    + "'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"tipo\":\"...\",\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CatalogoGeneroSearchRequest.class))))
    @PostMapping(value = "/catalogos/genero/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoGeneroResponse>>> buscar(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody(required = false) CatalogoGeneroSearchRequest request) {
        CatalogoGeneroSearchRequest filtros = request != null ? request : new CatalogoGeneroSearchRequest();
        Page<CatalogoGeneroResponse> generos = catalogoGeneroService.buscar(filtros);
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
    @PostMapping(value = "/catalogos/genero/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoGeneroResponse>> actualizar(
            @Valid @RequestBody CatalogoGeneroActualizaRequest request) {
        CatalogoGeneroResponse genero = catalogoGeneroService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, genero));
    }

    @Operation(summary = "Eliminar género")
    @PostMapping(value = "/catalogos/genero/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoGeneroService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
