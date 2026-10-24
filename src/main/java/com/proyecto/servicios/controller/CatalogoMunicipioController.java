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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo de Municipio", description = "Endpoints para la gestión del catálogo de municipios")
@RestController
public class CatalogoMunicipioController {

    private final CatalogoMunicipioService catalogoMunicipioService;

    public CatalogoMunicipioController(CatalogoMunicipioService catalogoMunicipioService) {
        this.catalogoMunicipioService = catalogoMunicipioService;
    }

    @Operation(summary = "Consultar municipio por ID",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Identificador del registro: {\"id\":1}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CatalogoIdRequest.class))))
    @PostMapping(value = "/catalogos/municipio/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipioResponse>> obtenerPorId(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody CatalogoIdRequest request) {
        CatalogoMunicipioResponse municipio = catalogoMunicipioService.obtenerPorId(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipio));
    }

    @Operation(summary = "Buscar municipios con filtros",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"nombre\":\"...\",\"estadoId\":1,\"pagina\":0,\"tamano\":10}. Sin cuerpo (o cuerpo vacío) "
                    + "devuelve todos los municipios paginados. 'nombre' aplica coincidencia parcial sin distinguir "
                    + "mayúsculas. La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta "
                    + "incluye 'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"nombre\":\"...\",\"estadoId\":1,\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = CatalogoMunicipioSearchRequest.class))))
    @PostMapping(value = "/catalogos/municipio/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<CatalogoMunicipioResponse>>> buscar(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
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
    @PostMapping(value = "/catalogos/municipio/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoMunicipioResponse>> actualizar(
            @Valid @RequestBody CatalogoMunicipioActualizaRequest request) {
        CatalogoMunicipioResponse municipio = catalogoMunicipioService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, municipio));
    }

    @Operation(summary = "Eliminar municipio")
    @PostMapping(value = "/catalogos/municipio/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminar(@Valid @RequestBody CatalogoIdRequest request) {
        catalogoMunicipioService.eliminar(request.getId());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, null));
    }
}
