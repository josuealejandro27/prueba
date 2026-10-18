package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasNombreRequest;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.model.PersonasSearchRequest;
import com.proyecto.servicios.model.pagination.PageResponse;
import com.proyecto.servicios.service.PersonaService;
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

@Tag(name = "Gestión de Personas", description = "Endpoints para la administración de personas")
@RestController
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @Operation(summary = "Consultar persona por nombre",
            description = "Método GET con el nombre enviado en el cuerpo (DTO) para respetar la "
                    + "restricción de no usar @PathVariable ni @RequestParam.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Nombre de la persona: {\"nombre\":\"Juan\"}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PersonasNombreRequest.class))))
    @GetMapping(value = "/personas/consultar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PersonaResponse>> obtenerPersona(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody PersonasNombreRequest request) {
        PersonaResponse persona = personaService.obtenerPersona(request.getNombre());
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, persona));
    }

    @Operation(summary = "Consultar personas con filtros y paginación",
            description = "Todos los criterios son opcionales y combinables y se envían en el cuerpo (DTO): "
                    + "{\"nombre\":\"...\",\"apellidoP\":\"...\",\"pagina\":0,\"tamano\":10}. "
                    + "Sin cuerpo (o cuerpo vacío) devuelve todas las personas paginadas. "
                    + "'nombre', 'apellidoP' y 'apellidoMaterno' aplican coincidencia parcial sin distinguir mayúsculas. "
                    + "La paginación se controla con 'pagina' (base 0) y 'tamano'; la respuesta incluye "
                    + "'pagina', 'totalPaginas', 'totalElementos' y 'esUltima'.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Criterios de búsqueda y paginación, todos opcionales. Ejemplo: {\"nombre\":\"...\",\"pagina\":1,\"tamano\":10}",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PersonasSearchRequest.class))))
    @GetMapping(value = "/personas/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PageResponse<PersonaResponse>>> buscarPersonas(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @Valid @RequestBody(required = false) PersonasSearchRequest request) {
        PersonasSearchRequest filtro = request != null ? request : new PersonasSearchRequest();
        Page<PersonaResponse> personas = personaService.buscarPersonas(filtro);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, PageResponse.desde(personas)));
    }

    @Operation(summary = "Crear persona")
    @PostMapping(value = "/personas/agregar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PersonaResponse>> crearPersona(
            @Valid @RequestBody PersonasRequest personasRequest) {
        PersonaResponse persona = personaService.creaPersona(personasRequest);
        return new ResponseEntity<>(ApiResponse.of(ApiResponseEnum.CREADO, persona), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar persona")
    @PutMapping(value = "/personas/actualizar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GenericResponse>> actualizarPersona(
            @Valid @RequestBody PersonasRequest personasRequest) {
        GenericResponse respuesta = personaService.actualizaPersona(personasRequest);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, respuesta));
    }

    @Operation(summary = "Eliminar persona")
    @PatchMapping(value = "/personas/eliminar", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GenericResponse>> eliminarPersona(
            @Valid @RequestBody EliminaPersonaRequest eliminaPersonaRequest) {
        GenericResponse respuesta = personaService.eliminaPersona(eliminaPersonaRequest);
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, respuesta));
    }
}
