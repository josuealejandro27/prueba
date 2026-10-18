package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilSearchRequest;
import com.proyecto.servicios.model.catalogo.CatalogoIdRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.CatalogoEstadoCivilService;
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

@Tag(name = "Catálogo de Estado Civil", description = "Endpoints para la gestión del catálogo de estados civiles")
@RestController
public class CatalogoEstadoCivilController {

    private final CatalogoEstadoCivilService catalogoEstadoCivilService;

    public CatalogoEstadoCivilController(CatalogoEstadoCivilService catalogoEstadoCivilService) {
        this.catalogoEstadoCivilService = catalogoEstadoCivilService;
    }

    @Operation(summary = "Consultar estado civil por ID",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Identificador del registro: {\"id\":1}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CatalogoIdRequest.class))))
    @GetMapping(value = "/catalogos/estado-civil/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoCivilResponse>> obtenerPorId(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoEstadoCivilResponse estadoCivil = catalogoEstadoCivilService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estadoCivil));
    }

    @Operation(summary = "Consultar estados civiles con filtros",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"nombre\":\"...\",\"pagina\":0,\"tamano\":10}. Sin cuerpo (o cuerpo vacío) devuelve "
                    + "todos los estados civiles paginados. 'nombre' aplica coincidencia parcial sin distinguir "
                    + "mayúsculas. La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta "
                    + "incluye 'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"nombre\":\"...\",\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CatalogoEstadoCivilSearchRequest.class))))
    @GetMapping(value = "/catalogos/estado-civil/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoEstadoCivilResponse>>> buscar(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody(required = false) CatalogoEstadoCivilSearchRequest request) {
        CatalogoEstadoCivilSearchRequest filtros = request != null ? request : new CatalogoEstadoCivilSearchRequest();
        Page<CatalogoEstadoCivilResponse> estadosCiviles = catalogoEstadoCivilService.buscar(filtros);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(estadosCiviles)));
    }

    @Operation(summary = "Crear estado civil")
    @PostMapping(value = "/catalogos/estado-civil/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoCivilResponse>> crear(
            @Valid @RequestBody CatalogoEstadoCivilRequest request) {
        CatalogoEstadoCivilResponse estadoCivil = catalogoEstadoCivilService.crear(request);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, estadoCivil), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar estado civil")
    @PutMapping(value = "/catalogos/estado-civil/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoEstadoCivilResponse>> actualizar(
            @Valid @RequestBody CatalogoEstadoCivilActualizaRequest request) {
        CatalogoEstadoCivilResponse estadoCivil = catalogoEstadoCivilService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, estadoCivil));
    }

    @Operation(summary = "Eliminar estado civil")
    @PatchMapping(value = "/catalogos/estado-civil/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoEstadoCivilService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
