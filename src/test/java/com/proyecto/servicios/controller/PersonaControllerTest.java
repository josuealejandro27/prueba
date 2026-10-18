package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.PersonaException;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasSearchRequest;
import com.proyecto.servicios.service.PersonaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica el contrato HTTP de los endpoints GET de {@code PersonaController}:
 * método GET con DTO en el cuerpo (sin {@code @PathVariable}/{@code @RequestParam}),
 * validación con {@code @Valid}, paginación y códigos de estado.
 */
@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    private static final String CONSULTAR = "/personas/consultar";
    private static final String BUSCAR = "/personas/buscar";

    @Mock
    private PersonaService personaService;

    private MockMvc mockMvc;
    private PersonaResponse personaResponse;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PersonaController(personaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        personaResponse = new PersonaResponse();
        personaResponse.setCodigo(1);
        personaResponse.setMensaje("Exito");
        personaResponse.setNombre("Juan");
        personaResponse.setApellidoP("Perez");
        personaResponse.setApellidoMaterno("Lopez");
    }

    @Test
    void buscarPersonas_FiltrosCombinados_PasaElDtoAlServicio() throws Exception {
        when(personaService.buscarPersonas(any(PersonasSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(personaResponse)));

        mockMvc.perform(get(BUSCAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\",\"apellidoP\":\"Perez\",\"pagina\":0,\"tamano\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[0].nombre").value("Juan"))
                .andExpect(jsonPath("$.data.contenido[0].apellidoP").value("Perez"))
                .andExpect(jsonPath("$.data.totalElementos").value(1));

        ArgumentCaptor<PersonasSearchRequest> captor = ArgumentCaptor.forClass(PersonasSearchRequest.class);
        verify(personaService).buscarPersonas(captor.capture());
        assertEquals("Juan", captor.getValue().getNombre());
        assertEquals("Perez", captor.getValue().getApellidoP());
        assertEquals(5, captor.getValue().getTamano());
    }

    @Test
    void buscarPersonas_SinCuerpo_Devuelve200ConPaginaPorDefecto() throws Exception {
        when(personaService.buscarPersonas(any(PersonasSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(personaResponse)));

        mockMvc.perform(get(BUSCAR).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido.length()").value(1));

        ArgumentCaptor<PersonasSearchRequest> captor = ArgumentCaptor.forClass(PersonasSearchRequest.class);
        verify(personaService).buscarPersonas(captor.capture());
        assertNull(captor.getValue().getNombre());
        assertEquals(0, captor.getValue().getPagina());
    }

    @Test
    void buscarPersonas_PaginaNegativa_Devuelve400() throws Exception {
        mockMvc.perform(get(BUSCAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagina\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(personaService, never()).buscarPersonas(any());
    }

    @Test
    void obtenerPersona_Existe_Devuelve200() throws Exception {
        when(personaService.obtenerPersona("Juan")).thenReturn(personaResponse);

        mockMvc.perform(get(CONSULTAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Juan"))
                .andExpect(jsonPath("$.data.apellidoMaterno").value("Lopez"));
    }

    @Test
    void obtenerPersona_NoExiste_Devuelve404() throws Exception {
        when(personaService.obtenerPersona("Pedro"))
                .thenThrow(new PersonaException("PERSONA_ERROR", "Persona no encontrada con nombre: Pedro",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(get(CONSULTAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pedro\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PERSONA_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void obtenerPersona_NombreEnBlanco_Devuelve400() throws Exception {
        mockMvc.perform(get(CONSULTAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(personaService, never()).obtenerPersona(any());
    }
}
