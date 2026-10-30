package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
@NoArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String CODIGO_USUARIO = "USUARIO_ERROR";
    private static final String CODIGO_CREDENCIALES = "CREDENCIALES_INVALIDAS";
    private static final String CODIGO_CLIENTE_INACTIVO = "CLIENTE_INACTIVO";
    private static final String CODIGO_LOGIN_BLOQUEADO = "LOGIN_BLOQUEADO";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Autowired
    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${jwt.secret:}") String jwtSecret,
                           @Value("${jwt.expiration:86400000}") long jwtExpiration) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Intento de login para correo: {}", request.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UsuarioException(CODIGO_CREDENCIALES, "Credenciales inválidas", HttpStatus.UNAUTHORIZED));

        if (!usuario.isActivo()) {
            throw new UsuarioException(CODIGO_USUARIO, "Usuario inactivo", HttpStatus.FORBIDDEN);
        }

        PersonaFisica cliente = usuario.getCliente();
        if (cliente == null) {
            throw new UsuarioException(CODIGO_USUARIO, "El usuario no tiene un cliente asociado válido", HttpStatus.FORBIDDEN);
        }

        if (!cliente.isActivo()) {
            throw new UsuarioException(CODIGO_CLIENTE_INACTIVO, "El cliente titular se encuentra inactivo", HttpStatus.FORBIDDEN);
        }

        if (cliente.isLoginBloqueado()) {
            throw new UsuarioException(CODIGO_LOGIN_BLOQUEADO, "El acceso del cliente se encuentra bloqueado", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new UsuarioException(CODIGO_CREDENCIALES, "Credenciales inválidas", HttpStatus.UNAUTHORIZED);
        }

        String token = generarToken(usuario);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsuarioId(usuario.getId());
        response.setCorreo(usuario.getCorreo());
        response.setNombre(cliente.getNombre() + " " + cliente.getApellidoPaterno());

        log.info("Login exitoso para usuario con ID: {}", usuario.getId());
        return response;
    }

    private String generarToken(Usuario usuario) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("La clave secreta JWT no está configurada o no cumple la longitud mínima de 256 bits");
        }
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(usuario.getCorreo())
                .claim("usuarioId", usuario.getId())
                .claim("clienteId", usuario.getCliente().getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
