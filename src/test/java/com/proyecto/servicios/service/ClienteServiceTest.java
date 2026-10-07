package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.*;
import com.proyecto.servicios.entity.clientes.*;
import com.proyecto.servicios.exception.ClienteException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.repositorys.catalogos.*;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private PersonaFisicaRepository personaFisicaRepository;

    @Mock
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Mock
    private CatalogoGenerosRepository catalogoGenerosRepository;

    @Mock
    private CatalogoPaisRepository catalogoPaisRepository;

    @Mock
    private CatalogoEstadoCivilRepository catalogoEstadoCivilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ClienteServiceImpl clienteService;

    private ClienteRequest request;
    private PersonaFisica personaFisica;
    private CuentaBancaria cuentaBancaria;

    @BeforeEach
    void setUp() throws Exception {
        clienteService = new ClienteServiceImpl();
        injectField("personaFisicaRepository", personaFisicaRepository);
        injectField("cuentaBancariaRepository", cuentaBancariaRepository);
        injectField("catalogoGenerosRepository", catalogoGenerosRepository);
        injectField("catalogoPaisRepository", catalogoPaisRepository);
        injectField("catalogoEstadoCivilRepository", catalogoEstadoCivilRepository);
        injectField("usuarioRepository", usuarioRepository);
        injectField("passwordEncoder", passwordEncoder);

        request = new ClienteRequest();
        request.setNombre("Juan");
        request.setApellidoPaterno("Pérez");
        request.setApellidoMaterno("García");
        request.setFechaNacimiento(new Date(946684800000L)); // 2000-01-01
        request.setCurp("PEGJ900101HDFRRN01");
        request.setRfc("PEGJ900101");
        request.setCorreo("juan@email.com");
        request.setLada((short) 55);
        request.setNumeroTelefono(1234567890);
        request.setCalle("Calle 123");
        request.setNoExterior((short) 123);
        request.setColonia("Colonia Centro");
        request.setMunicipio("Municipio Centro");
        request.setEstado("Estado Centro");
        request.setCp(12345);
        request.setPais("México");
        request.setOcupacion("Ingeniero");
        request.setEmpresa("Empresa SA");
        request.setIngresoMensual(new BigDecimal("5000.00"));
        request.setSaldoInicial(new BigDecimal("1000.00"));
        request.setPassword("Password123!");
        request.setGeneroId(1L);
        request.setNacionalidadId(1L);
        request.setEstadoCivilId(1L);

        personaFisica = new PersonaFisica();
        personaFisica.setId(1L);
        personaFisica.setNombre("Juan");
        personaFisica.setApellidoPaterno("Pérez");
        personaFisica.setApellidoMaterno("García");
        personaFisica.setCurp("PEGJ900101HDFRRN01");
        personaFisica.setRfc("PEGJ900101");
        personaFisica.setCorreo("juan@email.com");
        personaFisica.setActivo(true);

        cuentaBancaria = new CuentaBancaria();
        cuentaBancaria.setId(1L);
        cuentaBancaria.setNumeroCuenta("ABC1234567890123");
        cuentaBancaria.setSaldo(new BigDecimal("1000.00"));
        cuentaBancaria.setEstatus("ACTIVA");
        cuentaBancaria.setPersonaFisica(personaFisica);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = ClienteServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(clienteService, value);
    }

    @Test
    void crearCliente_RegistroExitoso() {
        when(personaFisicaRepository.existsByCurp(any())).thenReturn(false);
        when(personaFisicaRepository.existsByRfc(any())).thenReturn(false);
        when(personaFisicaRepository.existsByCorreo(any())).thenReturn(false);
        when(catalogoGenerosRepository.findById(1L)).thenReturn(Optional.of(new CatalogoGeneros()));
        when(catalogoPaisRepository.findById(1L)).thenReturn(Optional.of(new CatalogoPais()));
        when(catalogoEstadoCivilRepository.findById(1L)).thenReturn(Optional.of(new CatalogoEstadoCivil()));
        when(personaFisicaRepository.save(any())).thenReturn(personaFisica);
        when(cuentaBancariaRepository.save(any())).thenReturn(cuentaBancaria);
        when(passwordEncoder.encode(any())).thenReturn("encryptedPassword");
        when(usuarioRepository.save(any())).thenReturn(new Usuario());

        ClienteResponse response = clienteService.crearCliente(request);

        assertNotNull(response);
        assertEquals("Juan", response.getNombre());
        assertEquals("PEGJ900101HDFRRN01", response.getCurp());
        verify(personaFisicaRepository).save(any());
        verify(cuentaBancariaRepository).save(any());
        verify(usuarioRepository).save(any());
    }

    @Test
    void crearCliente_CurpDuplicada() {
        when(personaFisicaRepository.existsByCurp(any())).thenReturn(true);

        assertThrows(ClienteException.class, () -> clienteService.crearCliente(request));
    }

    @Test
    void crearCliente_RfcDuplicado() {
        when(personaFisicaRepository.existsByCurp(any())).thenReturn(false);
        when(personaFisicaRepository.existsByRfc(any())).thenReturn(true);

        assertThrows(ClienteException.class, () -> clienteService.crearCliente(request));
    }

    @Test
    void crearCliente_CorreoDuplicado() {
        when(personaFisicaRepository.existsByCurp(any())).thenReturn(false);
        when(personaFisicaRepository.existsByRfc(any())).thenReturn(false);
        when(personaFisicaRepository.existsByCorreo(any())).thenReturn(true);

        assertThrows(ClienteException.class, () -> clienteService.crearCliente(request));
    }

    @Test
    void crearCliente_MenorDeEdad() {
        request.setFechaNacimiento(new Date()); // Fecha actual = menor de edad

        assertThrows(ValidationException.class, () -> clienteService.crearCliente(request));
    }

    @Test
    void crearCliente_CurpNormalizada() {
        request.setCurp("pegj900101hdfrrn01"); // minúsculas
        when(personaFisicaRepository.existsByCurp("PEGJ900101HDFRRN01")).thenReturn(false);
        when(personaFisicaRepository.existsByRfc(any())).thenReturn(false);
        when(personaFisicaRepository.existsByCorreo(any())).thenReturn(false);
        when(catalogoGenerosRepository.findById(1L)).thenReturn(Optional.of(new CatalogoGeneros()));
        when(catalogoPaisRepository.findById(1L)).thenReturn(Optional.of(new CatalogoPais()));
        when(catalogoEstadoCivilRepository.findById(1L)).thenReturn(Optional.of(new CatalogoEstadoCivil()));
        when(personaFisicaRepository.save(any())).thenReturn(personaFisica);
        when(cuentaBancariaRepository.save(any())).thenReturn(cuentaBancaria);
        when(passwordEncoder.encode(any())).thenReturn("encryptedPassword");
        when(usuarioRepository.save(any())).thenReturn(new Usuario());

        clienteService.crearCliente(request);

        verify(personaFisicaRepository).existsByCurp("PEGJ900101HDFRRN01");
    }

    @Test
    void desactivarCliente_DesactivaUsuario() {
        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setActivo(true);
        
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(personaFisica));
        when(cuentaBancariaRepository.findByPersonaFisicaId(1L)).thenReturn(Optional.of(cuentaBancaria));
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.of(usuarioMock));
        when(personaFisicaRepository.save(any())).thenReturn(personaFisica);
        when(cuentaBancariaRepository.save(any())).thenReturn(cuentaBancaria);
        when(usuarioRepository.save(any())).thenReturn(usuarioMock);

        clienteService.desactivarCliente(1L);

        assertFalse(personaFisica.isActivo());
        assertEquals("CANCELADA", cuentaBancaria.getEstatus());
        verify(usuarioRepository).save(any());
    }
}
