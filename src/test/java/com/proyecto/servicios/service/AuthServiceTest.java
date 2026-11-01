package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    private LoginRequest request;
    private Usuario usuario;
    private PersonaFisica persona;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                usuarioRepository,
                passwordEncoder,
                "clave_jwt_de_prueba_con_al_menos_32_caracteres_2024",
                86400000L
        );

        request = new LoginRequest();
        request.setCorreo("juan@email.com");
        request.setPassword("Password123!");

        persona = new PersonaFisica();
        persona.setId(10L);
        persona.setNombre("Juan");
        persona.setApellidoPaterno("Pérez");
        persona.setActivo(true);
        persona.setLoginBloqueado(false);
        persona.setCuentaBloqueada(false);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreo("juan@email.com");
        usuario.setPassword("encryptedPassword");
        usuario.setActivo(true);
        usuario.setCliente(persona);
    }

    @Test
    @DisplayName("Login exitoso con cliente y usuario activos")
    void login_CredencialesCorrectas() {
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "encryptedPassword")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("juan@email.com", response.getCorreo());
        assertEquals("Juan Pérez", response.getNombre());
    }

    @Test
    @DisplayName("Login falla cuando el usuario no existe")
    void login_UsuarioNoEncontrado() {
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login falla cuando el usuario está inactivo")
    void login_UsuarioInactivo() {
        usuario.setActivo(false);
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));

        UsuarioException ex = assertThrows(UsuarioException.class, () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Login falla cuando el cliente titular está inactivo")
    void login_ClienteInactivo_RechazaCon403() {
        persona.setActivo(false);
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));

        UsuarioException ex = assertThrows(UsuarioException.class, () -> authService.login(request));
        assertEquals("CLIENTE_INACTIVO", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Login falla cuando el cliente titular tiene loginBloqueado = true")
    void login_ClienteConLoginBloqueado_RechazaCon403() {
        persona.setLoginBloqueado(true);
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));

        UsuarioException ex = assertThrows(UsuarioException.class, () -> authService.login(request));
        assertEquals("LOGIN_BLOQUEADO", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    @DisplayName("Login falla con contraseña incorrecta")
    void login_ContrasenaIncorrecta() {
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "encryptedPassword")).thenReturn(false);

        assertThrows(UsuarioException.class, () -> authService.login(request));
    }
}
