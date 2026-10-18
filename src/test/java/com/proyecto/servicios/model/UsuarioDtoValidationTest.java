package com.proyecto.servicios.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioIdRequest;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.model.clientes.UsuarioUpdateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica las restricciones de Bean Validation de los DTO de entrada de usuarios.
 */
class UsuarioDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Set<String> camposInvalidos(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation<Object>::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    // ---------- UsuarioCreateRequest ----------

    @Test
    void crearRequest_DatosValidos_NoTieneViolaciones() {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(1L);
        request.setCorreo("juan@email.com");
        request.setPassword("Password123!");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void crearRequest_CamposObligatoriosFaltantes_MarcaLosTres() {
        Set<String> campos = camposInvalidos(new UsuarioCreateRequest());

        assertTrue(campos.contains("clienteId"));
        assertTrue(campos.contains("correo"));
        assertTrue(campos.contains("password"));
    }

    @Test
    void crearRequest_CorreoInvalido_MarcaCorreo() {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(1L);
        request.setCorreo("no-es-un-correo");
        request.setPassword("Password123!");

        assertTrue(camposInvalidos(request).contains("correo"));
    }

    @Test
    void crearRequest_PasswordDebil_MarcaPassword() {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(1L);
        request.setCorreo("juan@email.com");
        request.setPassword("weak");

        assertTrue(camposInvalidos(request).contains("password"));
    }

    @Test
    void crearRequest_ClienteNegativo_MarcaClienteId() {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(-1L);
        request.setCorreo("juan@email.com");
        request.setPassword("Password123!");

        assertTrue(camposInvalidos(request).contains("clienteId"));
    }

    // ---------- UsuarioUpdateRequest ----------

    @Test
    void updateRequest_SoloId_NoTieneViolaciones() {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(1L);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void updateRequest_IdNulo_MarcaId() {
        Set<String> campos = camposInvalidos(new UsuarioUpdateRequest());

        assertTrue(campos.contains("id"));
    }

    @Test
    void updateRequest_PasswordDebil_MarcaPassword() {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(1L);
        request.setPassword("weak");

        assertTrue(camposInvalidos(request).contains("password"));
    }

    // ---------- UsuarioSearchRequest ----------

    @Test
    void searchRequest_Vacio_NoTieneViolaciones() {
        assertTrue(validator.validate(new UsuarioSearchRequest()).isEmpty());
    }

    @Test
    void searchRequest_CorreoParcialEsValido() {
        UsuarioSearchRequest request = new UsuarioSearchRequest();
        request.setCorreo("juan");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void searchRequest_CorreoDemasiadoLargo_MarcaCorreo() {
        UsuarioSearchRequest request = new UsuarioSearchRequest();
        request.setCorreo("a".repeat(101));

        assertTrue(camposInvalidos(request).contains("correo"));
    }

    // ---------- UsuarioIdRequest ----------

    @Test
    void idRequest_DatosValidos_NoTieneViolaciones() {
        UsuarioIdRequest request = new UsuarioIdRequest();
        request.setId(5L);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void idRequest_IdNulo_MarcaId() {
        assertTrue(camposInvalidos(new UsuarioIdRequest()).contains("id"));
    }

    // ---------- Protección de campos controlados por el servidor ----------

    @Test
    void dtosDeEntrada_NoDeclaranCamposControladosPorElServidor() {
        for (String campo : new String[]{"id", "activo", "fechaCreacion", "fechaActualizacion", "cliente"}) {
            assertFalse(declaraCampo(UsuarioCreateRequest.class, campo),
                    "UsuarioCreateRequest no debe declarar el campo protegido: " + campo);
        }
        for (String campo : new String[]{"clienteId", "activo", "fechaCreacion", "fechaActualizacion", "cliente"}) {
            assertFalse(declaraCampo(UsuarioUpdateRequest.class, campo),
                    "UsuarioUpdateRequest no debe declarar el campo protegido: " + campo);
        }
    }

    @Test
    void crearRequest_IgnoraPropiedadesControladasPorElServidorEnElJson() throws Exception {
        // Spring Boot desactiva FAIL_ON_UNKNOWN_PROPERTIES: las propiedades extra se ignoran.
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        UsuarioCreateRequest request = mapper.readValue(
                "{\"clienteId\":1,\"correo\":\"juan@email.com\",\"password\":\"Password123!\","
                        + "\"id\":99,\"activo\":false,"
                        + "\"fechaCreacion\":\"2000-01-01T00:00:00\",\"fechaActualizacion\":\"2000-01-01T00:00:00\"}",
                UsuarioCreateRequest.class);

        assertEquals(1L, request.getClienteId());
        assertEquals("juan@email.com", request.getCorreo());
        assertEquals("Password123!", request.getPassword());
        assertTrue(validator.validate(request).isEmpty());
    }

    private static boolean declaraCampo(Class<?> tipo, String nombre) {
        return java.util.Arrays.stream(tipo.getDeclaredFields())
                .anyMatch(f -> f.getName().equals(nombre));
    }
}
