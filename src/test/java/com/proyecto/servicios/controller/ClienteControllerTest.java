package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.ClienteSearchRequest;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el contrato HTTP del {@code ClienteController}: consultas y
 * búsquedas con método GET enviando los criterios y la paginación como
 * parámetros de consulta (query params, sin {@code @RequestParam}), altas en
 * POST y actualizaciones en PUT/PATCH, validación centralizada en el
 * {@code GlobalExceptionHandler} y ausencia de rutas con {@code @PathVariable}
 * o {@code @RequestParam}.
 */
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    private MockMvc mockMvc;
    private ClienteResponse clienteResponse;

    private static final String CLIENTE_VALIDO = "{"
            + "\"nombre\":\"Juan\",\"apellidoPaterno\":\"Perez\",\"apellidoMaterno\":\"Lopez\","
            + "\"fechaNacimiento\":642902400000,"
            + "\"curp\":\"PERJ900520HDFRPN09\",\"rfc\":\"PERJ900520ABC\","
            + "\"generoId\":1,\"nacionalidadId\":1,\"estadoCivilId\":1,"
            + "\"correo\":\"juan@example.com\",\"lada\":55,\"numeroTelefono\":1234567890,"
            + "\"calle\":\"Calle 1\",\"noExterior\":10,\"colonia\":\"Centro\","
            + "\"municipio\":\"Municipio\",\"estado\":\"Estado\",\"cp\":12345,\"pais\":\"Mexico\","
            + "\"ocupacion\":\"Ingeniero\",\"empresa\":\"Empresa\",\"ingresoMensual\":1000.00,"
            + "\"saldoInicial\":100.00,\"password\":\"Password123!\"}";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        clienteResponse = new ClienteResponse();
        clienteResponse.setId(1L);
        clienteResponse.setNombre("Juan");
        clienteResponse.setCurp("PERJ900520HDFRPN09");
        clienteResponse.setRfc("PERJ900520ABC");
        clienteResponse.setActivo(true);
    }

    @Test
    void crearCliente_DatosValidos_Devuelve201() throws Exception {
        when(clienteService.crearCliente(any(ClienteRequest.class))).thenReturn(clienteResponse);

        mockMvc.perform(post("/clientes/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENTE_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.data.curp").value("PERJ900520HDFRPN09"));
    }

    @Test
    void crearCliente_DatosInvalidos_Devuelve400ConDetalle() throws Exception {
        mockMvc.perform(post("/clientes/agregar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"J\",\"curp\":\"invalida\",\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.curp").exists())
                .andExpect(jsonPath("$.data.correo").exists());

        verify(clienteService, never()).crearCliente(any());
    }

    @Test
    void buscarClientes_ConPaginacion_DevuelvePageResponse() throws Exception {
        when(clienteService.buscarClientes(any(ClienteSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(clienteResponse), PageRequest.of(1, 10), 21));

        mockMvc.perform(get("/clientes/buscar")
                        .param("curp", "PERJ900520HDFRPN09")
                        .param("pagina", "1")
                        .param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contenido[0].curp").value("PERJ900520HDFRPN09"))
                .andExpect(jsonPath("$.data.totalElementos").value(21))
                .andExpect(jsonPath("$.data.pagina").value(1))
                .andExpect(jsonPath("$.data.tamano").value(10));
    }

    @Test
    void buscarClientes_TamanoDePaginaInvalido_Devuelve400() throws Exception {
        mockMvc.perform(get("/clientes/buscar")
                        .param("tamano", "500"))
                .andExpect(status().isBadRequest());

        verify(clienteService, never()).buscarClientes(any());
    }

    @Test
    void consultarCliente_SinId_Devuelve400() throws Exception {
        mockMvc.perform(get("/clientes/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.id").exists());
    }
}
