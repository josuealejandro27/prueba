package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica el contrato HTTP del {@code UsuarioController}:
 * validación con {@code @Valid}, códigos de estado, manejo global de errores
 * y que los campos controlados por el servidor no sean aceptados del cliente.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mockMvc;
    private UsuarioResponse usuarioResponse;

    @BeforeEach
    void setUp() throws Exception {
        // Misma tolerancia a propiedades desconocidas y mismo módulo de fechas que la aplicación real.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsuarioController(usuarioService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        PersonaFisica persona = new PersonaFisica();
        persona.setId(1L);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(persona);
        usuario.setCorreo("juan@email.com");
        usuario.setActivo(true);
        usuario.setFechaCreacion(new Date());
        usuario.setFechaActualizacion(new Date());

        usuarioResponse = new UsuarioResponse();
        usuarioResponse.setId(1L);
        usuarioResponse.setClienteId(1L);
        usuarioResponse.setCorreo("juan@email.com");
        usuarioResponse.setActivo(true);
    }

    @Test
    void crearUsuario_DatosValidos_Devuelve201() throws Exception {
        when(usuarioService.crearUsuario(any(UsuarioCreateRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(post("/usuarios/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"correo\":\"juan@email.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.correo").value("juan@email.com"));
    }

    @Test
    void crearUsuario_DatosInvalidos_Devuelve400ConDetalle() throws Exception {
        mockMvc.perform(post("/usuarios/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"correo\":\"no-es-correo\",\"password\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.correo").exists())
                .andExpect(jsonPath("$.data.password").exists());

        verify(usuarioService, never()).crearUsuario(any());
    }

    @Test
    void crearUsuario_CamposProtegidosNoSonAceptados() throws Exception {
        // El cliente envía 'activo' y 'id', que son campos controlados por el servidor:
        // deben ignorarse (no hay setters en el DTO) y el servicio recibe solo lo permitido.
        when(usuarioService.crearUsuario(any(UsuarioCreateRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(post("/usuarios/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"correo\":\"juan@email.com\",\"password\":\"Password123!\","
                                + "\"activo\":false,\"id\":99,\"fechaCreacion\":\"2000-01-01\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<UsuarioCreateRequest> captor = ArgumentCaptor.forClass(UsuarioCreateRequest.class);
        verify(usuarioService).crearUsuario(captor.capture());
        assertEquals(1L, captor.getValue().getClienteId());
        assertEquals("juan@email.com", captor.getValue().getCorreo());
        assertEquals("Password123!", captor.getValue().getPassword());
    }

    @Test
    void buscarUsuarios_CuerpoInvalido_Devuelve400() throws Exception {
        mockMvc.perform(post("/usuarios/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(usuarioService, never()).buscarUsuarios(any());
    }

    @Test
    void buscarUsuarios_FiltrosCombinados_PasaElDtoAlServicio() throws Exception {
        when(usuarioService.buscarUsuarios(any(UsuarioSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(usuarioResponse), PageRequest.of(0, 10), 1));

        mockMvc.perform(post("/usuarios/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"juan\",\"clienteId\":1,\"activos\":true,\"pagina\":0,\"tamano\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[0].correo").value("juan@email.com"))
                .andExpect(jsonPath("$.data.pagina").value(0))
                .andExpect(jsonPath("$.data.tamano").value(10))
                .andExpect(jsonPath("$.data.totalPaginas").value(1));

        ArgumentCaptor<UsuarioSearchRequest> captor = ArgumentCaptor.forClass(UsuarioSearchRequest.class);
        verify(usuarioService).buscarUsuarios(captor.capture());
        assertEquals("juan", captor.getValue().getCorreo());
        assertEquals(1L, captor.getValue().getClienteId());
        assertTrue(captor.getValue().getActivos());
        assertEquals(0, captor.getValue().getPagina());
        assertEquals(10, captor.getValue().getTamano());
    }

    @Test
    void buscarUsuarios_SinFiltros_Devuelve200() throws Exception {
        when(usuarioService.buscarUsuarios(any(UsuarioSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(usuarioResponse)));

        mockMvc.perform(post("/usuarios/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido.length()").value(1))
                .andExpect(jsonPath("$.data.pagina").value(0));
    }

    @Test
    void obtenerUsuario_NoExistente_Devuelve404() throws Exception {
        when(usuarioService.obtenerUsuario(99L))
                .thenThrow(new UsuarioNoEncontradoException(99L));

        mockMvc.perform(post("/usuarios/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USUARIO_NO_ENCONTRADO"));
    }

    @Test
    void desactivarUsuario_DatosValidos_Devuelve200() throws Exception {
        when(usuarioService.desactivarUsuario(1L)).thenReturn(usuarioResponse);

        mockMvc.perform(post("/usuarios/desactivar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void activarUsuario_IdNulo_Devuelve400() throws Exception {
        mockMvc.perform(post("/usuarios/activar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(usuarioService, never()).activarUsuario(any());
    }

    @Test
    void crearUsuario_CorreoDuplicado_Devuelve409ConEstructuraUniforme() throws Exception {
        when(usuarioService.crearUsuario(any(UsuarioCreateRequest.class)))
                .thenThrow(new CorreoDuplicadoException("juan@email.com"));

        mockMvc.perform(post("/usuarios/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"correo\":\"juan@email.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORREO_DUPLICADO"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void errorInesperado_Devuelve500ConEstructuraUniforme() throws Exception {
        when(usuarioService.obtenerUsuario(1L)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/usuarios/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No se pudo completar la operación")));
    }
}
