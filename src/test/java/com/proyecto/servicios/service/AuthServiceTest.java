package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
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

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthServiceImpl();
        injectField("usuarioRepository", usuarioRepository);
        injectField("passwordEncoder", passwordEncoder);
        injectField("jwtSecret", "miClaveSecretaSuperSeguraParaJWT2024");
        injectField("jwtExpiration", 86400000L);

        request = new LoginRequest();
        request.setCorreo("juan@email.com");
        request.setPassword("Password123!");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreo("juan@email.com");
        usuario.setPassword("encryptedPassword");
        usuario.setActivo(true);

        PersonaFisica persona = new PersonaFisica();
        persona.setNombre("Juan");
        persona.setApellidoPaterno("Pérez");
        usuario.setCliente(persona);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AuthServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(authService, value);
    }

    @Test
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
    void login_UsuarioNoEncontrado() {
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioException.class, () -> authService.login(request));
    }

    @Test
    void login_UsuarioInactivo() {
        usuario.setActivo(false);
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioException.class, () -> authService.login(request));
    }

    @Test
    void login_ContrasenaIncorrecta() {
        when(usuarioRepository.findByCorreo("juan@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "encryptedPassword")).thenReturn(false);

        assertThrows(UsuarioException.class, () -> authService.login(request));
    }
}
