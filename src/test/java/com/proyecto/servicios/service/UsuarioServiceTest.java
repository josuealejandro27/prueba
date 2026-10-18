package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.model.clientes.UsuarioUpdateRequest;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PersonaFisicaRepository personaFisicaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;
    private PersonaFisica persona;

    @BeforeEach
    void setUp() throws Exception {
        usuarioService = new UsuarioServiceImpl();
        injectField("usuarioRepository", usuarioRepository);
        injectField("personaFisicaRepository", personaFisicaRepository);
        injectField("passwordEncoder", passwordEncoder);

        persona = new PersonaFisica();
        persona.setId(1L);
        persona.setNombre("Juan");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(persona);
        usuario.setCorreo("juan@email.com");
        usuario.setPassword("encryptedPassword");
        usuario.setActivo(true);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = UsuarioServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(usuarioService, value);
    }

    private UsuarioCreateRequest crearRequest(String correo, String password) {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setClienteId(1L);
        request.setCorreo(correo);
        request.setPassword(password);
        return request;
    }

    @Test
    void crearUsuario_CreacionExitosa() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encryptedPassword");
        when(usuarioRepository.save(any())).thenReturn(usuario);

        UsuarioResponse response = usuarioService.crearUsuario(crearRequest("juan@email.com", "Password123!"));

        assertNotNull(response);
        assertEquals("juan@email.com", response.getCorreo());
        assertTrue(response.isActivo());
    }

    @Test
    void crearUsuario_CorreoDuplicado() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(true);

        assertThrows(UsuarioException.class,
                () -> usuarioService.crearUsuario(crearRequest("juan@email.com", "Password123!")));
    }

    @Test
    void crearUsuario_ClienteConUsuario() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(true);

        assertThrows(UsuarioException.class,
                () -> usuarioService.crearUsuario(crearRequest("juan@email.com", "Password123!")));
    }

    @Test
    void crearUsuario_ClienteInexistente() {
        when(personaFisicaRepository.findById(99L)).thenReturn(Optional.empty());

        UsuarioCreateRequest request = crearRequest("juan@email.com", "Password123!");
        request.setClienteId(99L);

        assertThrows(RuntimeException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    void buscarUsuarios_SinFiltros_DevuelveTodos() {
        when(usuarioRepository.findAll(ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario)));

        Page<UsuarioResponse> resultados = usuarioService.buscarUsuarios(new UsuarioSearchRequest());

        assertEquals(1, resultados.getContent().size());
        assertEquals("juan@email.com", resultados.getContent().get(0).getCorreo());
        verify(usuarioRepository).findAll(ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class));
    }

    @Test
    void buscarUsuarios_ConFiltrosCombinados() {
        when(usuarioRepository.findAll(ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario)));

        UsuarioSearchRequest filtros = new UsuarioSearchRequest();
        filtros.setCorreo("juan");
        filtros.setClienteId(1L);
        filtros.setActivos(true);

        Page<UsuarioResponse> resultados = usuarioService.buscarUsuarios(filtros);

        assertEquals(1, resultados.getContent().size());
        verify(usuarioRepository).findAll(ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class));
    }

    @Test
    void actualizarUsuario_CorreoDuplicado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@email.com")).thenReturn(true);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(1L);
        request.setCorreo("otro@email.com");

        assertThrows(UsuarioException.class, () -> usuarioService.actualizarUsuario(request));
    }

    @Test
    void actualizarUsuario_UsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(99L);
        request.setCorreo("nuevo@email.com");

        assertThrows(UsuarioException.class, () -> usuarioService.actualizarUsuario(request));
    }

    @Test
    void obtenerUsuario_NoExistente_LanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioException.class, () -> usuarioService.obtenerUsuario(99L));
    }

    @Test
    void desactivarUsuario_DesactivaCorrectamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        UsuarioResponse response = usuarioService.desactivarUsuario(1L);

        assertFalse(response.isActivo());
        verify(usuarioRepository).save(any());
    }

    @Test
    void activarUsuario_ActivaCorrectamente() {
        usuario.setActivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        UsuarioResponse response = usuarioService.activarUsuario(1L);

        assertTrue(response.isActivo());
        verify(usuarioRepository).save(any());
    }

    @Test
    void crearUsuario_FuerzaActivoTrueYNoTocaCamposDelServidor() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-xyz");

        Usuario guardado = new Usuario();
        guardado.setCliente(persona);
        guardado.setCorreo("juan@email.com");
        guardado.setActivo(true);
        when(usuarioRepository.save(any())).thenReturn(guardado);

        usuarioService.crearUsuario(crearRequest("juan@email.com", "Password123!"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario entidad = captor.getValue();

        assertTrue(entidad.isActivo(), "activo debe forzarse a true en el servidor");
        assertEquals("encoded-xyz", entidad.getPassword(), "la contraseña debe persistirse cifrada");
        assertNull(entidad.getFechaCreacion(), "fechaCreacion la asigna JPA, no el servicio");
        assertNull(entidad.getFechaActualizacion(), "fechaActualizacion la asigna JPA, no el servicio");
    }

    @Test
    void actualizarUsuario_NoAlteraCamposControladosPorElServidor() {
        Date creacion = new Date(1_500_000_000_000L);
        Date actualizacion = new Date(1_600_000_000_000L);
        usuario.setActivo(false);
        usuario.setFechaCreacion(creacion);
        usuario.setFechaActualizacion(actualizacion);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(passwordEncoder.encode("NuevaPass1!")).thenReturn("encoded-nueva");

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setId(1L);
        request.setCorreo("nuevo@email.com");
        request.setPassword("NuevaPass1!");

        usuarioService.actualizarUsuario(request);

        assertFalse(usuario.isActivo(), "actualizar no debe reactivar al usuario");
        assertEquals("nuevo@email.com", usuario.getCorreo());
        assertEquals("encoded-nueva", usuario.getPassword());
        assertEquals(creacion, usuario.getFechaCreacion(), "fechaCreacion no debe cambiar");
        assertEquals(actualizacion, usuario.getFechaActualizacion(), "fechaActualizacion no la fija el servicio");
    }
}
