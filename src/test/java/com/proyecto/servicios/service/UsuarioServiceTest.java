package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.Impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;
    private PersonaFisica persona;

    @BeforeEach
    void setUp() {
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

    @Test
    void crearUsuario_CreacionExitosa() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encryptedPassword");
        when(usuarioRepository.save(any())).thenReturn(usuario);

        UsuarioResponse response = usuarioService.crearUsuario(1L, "juan@email.com", "Password123!");

        assertNotNull(response);
        assertEquals("juan@email.com", response.getCorreo());
        assertTrue(response.isActivo());
    }

    @Test
    void crearUsuario_CorreoDuplicado() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(true);

        assertThrows(UsuarioException.class, () -> usuarioService.crearUsuario(1L, "juan@email.com", "Password123!"));
    }

    @Test
    void crearUsuario_ClienteConUsuario() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(true);

        assertThrows(UsuarioException.class, () -> usuarioService.crearUsuario(1L, "juan@email.com", "Password123!"));
    }

    @Test
    void crearUsuario_ContrasenaInvalida() {
        when(personaFisicaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByCorreo("juan@email.com")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(1L)).thenReturn(false);

        assertThrows(ValidationException.class, () -> usuarioService.crearUsuario(1L, "juan@email.com", "weak"));
    }

    @Test
    void desactivarUsuario_DesactivaCorrectamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.desactivarUsuario(1L);

        assertFalse(response.isActivo());
        verify(usuarioRepository).save(any());
    }
}
