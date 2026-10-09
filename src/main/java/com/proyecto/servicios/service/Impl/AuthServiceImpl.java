package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
public class AuthServiceImpl implements AuthService {

    private static final String CODIGO_USUARIO = "USUARIO_ERROR";
    private static final String CODIGO_CREDENCIALES = "CREDENCIALES_INVALIDAS";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret:miClaveSecretaSuperSeguraParaJWT2024}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Intento de login para correo: {}", request.getCorreo());

        // Buscar usuario por correo
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UsuarioException(CODIGO_CREDENCIALES, "Credenciales inválidas", HttpStatus.UNAUTHORIZED));

        // Validar que el usuario esté activo
        if (!usuario.isActivo()) {
            throw new UsuarioException(CODIGO_USUARIO, "Usuario inactivo", HttpStatus.FORBIDDEN);
        }

        // Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new UsuarioException(CODIGO_CREDENCIALES, "Credenciales inválidas", HttpStatus.UNAUTHORIZED);
        }

        // Generar token JWT
        String token = generarToken(usuario);

        // Construir respuesta
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsuarioId(usuario.getId());
        response.setCorreo(usuario.getCorreo());
        response.setNombre(usuario.getCliente().getNombre() + " " + usuario.getCliente().getApellidoPaterno());

        log.info("Login exitoso para usuario con ID: {}", usuario.getId());
        return response;
    }

    private String generarToken(Usuario usuario) {
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
