package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.service.PagoFinancieroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas del controlador TransaccionController")
class TransaccionControllerTest {

    @Mock
    private PagoFinancieroService pagoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TransaccionController(pagoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private PagoRequest requestValido() {
        return PagoRequest.builder()
                .numeroCuenta("1234567890123456")
                .idServicio(105L)
                .idProducto(201L)
                .monto(new BigDecimal("100.00"))
                .idDistribuidor(83)
                .referencia("5512345678")
                .bandera("T")
                .pin("Password123!")
                .upc("123456789012")
                .idempotencyKey("idemp-test-001")
                .build();
    }

    @Test
    @DisplayName("Pagar: transacción aprobada devuelve 200 OK con datos completos")
    void pagar_TransaccionExitosa_Retorna200() throws Exception {
        PagoResponse response = PagoResponse.builder()
                .folio("folio-uuid-1234")
                .idempotencyKey("idemp-test-001")
                .numeroCuenta("1234567890123456")
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuestaProveedor("0")
                .mensajeProveedor("TRANSACCION EXITOSA")
                .folioProveedor("AUT-778899")
                .saldoRestante(new BigDecimal("400.00"))
                .fechaTransaccion(LocalDateTime.now())
                .build();

        when(pagoService.procesarPago(any(PagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transacciones/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.folio").value("folio-uuid-1234"))
                .andExpect(jsonPath("$.data.estado").value("EXITOSA"))
                .andExpect(jsonPath("$.data.folioProveedor").value("AUT-778899"));
    }

    @Test
    @DisplayName("Pagar: datos inválidos en el cuerpo devuelven 400 Bad Request con detalle de campos")
    void pagar_DatosInvalidos_Retorna400() throws Exception {
        PagoRequest invalido = PagoRequest.builder()
                .numeroCuenta("123") // Menor a 16 caracteres
                .monto(new BigDecimal("-10.00")) // Negativo
                .build();

        mockMvc.perform(post("/transacciones/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.numeroCuenta").exists())
                .andExpect(jsonPath("$.data.monto").exists());

        verify(pagoService, never()).procesarPago(any());
    }

    @Test
    @DisplayName("Pagar: transacción rechazada por el proveedor devuelve 422 Unprocessable Entity")
    void pagar_TransaccionRechazada_Retorna422() throws Exception {
        PagoResponse response = PagoResponse.builder()
                .folio("folio-uuid-5678")
                .idempotencyKey("idemp-test-002")
                .numeroCuenta("1234567890123456")
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.RECHAZADA)
                .codigoRespuestaProveedor("ERR-99")
                .mensajeProveedor("SALDO INSUFICIENTE EN DISPOSITIVO GESTOPAGO")
                .saldoRestante(new BigDecimal("500.00"))
                .fechaTransaccion(LocalDateTime.now())
                .build();

        when(pagoService.procesarPago(any(PagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transacciones/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSACCION_RECHAZADA"))
                .andExpect(jsonPath("$.data.estado").value("RECHAZADA"));
    }

    @Test
    @DisplayName("Pagar: saldo insuficiente en la cuenta lanza 422 con código uniforme")
    void pagar_SaldoInsuficiente_Retorna422() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class)))
                .thenThrow(new SaldoInsuficienteException("1234567890123456", new BigDecimal("50.00"), new BigDecimal("100.00")));

        mockMvc.perform(post("/transacciones/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SALDO_INSUFICIENTE"));
    }

    @Test
    @DisplayName("Pagar: cuenta no activa lanza 422 con código uniforme")
    void pagar_CuentaInactiva_Retorna422() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class)))
                .thenThrow(new CuentaInactivaException("1234567890123456", "BLOQUEADA"));

        mockMvc.perform(post("/transacciones/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CUENTA_NO_ACTIVA"));
    }

    @Test
    @DisplayName("Pagar: PIN incorrecto lanza 401 Unauthorized")
    void pagar_PinInvalido_Retorna401() throws Exception {
        when(pagoService.procesarPago(any(PagoRequest.class)))
                .thenThrow(new PinInvalidoException());

        mockMvc.perform(post("/transacciones/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PIN_INVALIDO"));
    }

    @Test
    @DisplayName("Consultar: folio existente devuelve 200 OK con los datos de la transacción")
    void consultar_FolioValido_Retorna200() throws Exception {
        ConsultaTransaccionRequest request = new ConsultaTransaccionRequest("folio-123");
        PagoResponse response = PagoResponse.builder()
                .folio("folio-123")
                .estado(EstadoTransaccion.EXITOSA)
                .monto(new BigDecimal("100.00"))
                .build();

        when(pagoService.consultarTransaccion(any(ConsultaTransaccionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transacciones/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.folio").value("folio-123"));
    }

    @Test
    @DisplayName("Consultar: folio vacío devuelve 400 Bad Request por validación")
    void consultar_FolioVacio_Retorna400() throws Exception {
        ConsultaTransaccionRequest request = new ConsultaTransaccionRequest("");

        mockMvc.perform(post("/transacciones/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(pagoService, never()).consultarTransaccion(any());
    }
}

