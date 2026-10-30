package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

/**
 * Filtro de autenticación JWT.
 * Valida la firma, expiración y estado activo del usuario/cliente en cada petición.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Autowired(required = false)
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("Configuración de clave JWT inválida");
            }
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String correo = claims.getSubject();

            if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (usuarioRepository != null) {
                    Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoWithCliente(correo);
                    if (usuarioOpt.isPresent()) {
                        Usuario u = usuarioOpt.get();
                        PersonaFisica c = u.getCliente();
                        if (!u.isActivo() || (c != null && (!c.isActivo() || c.isLoginBloqueado()))) {
                            log.warn("Acceso JWT denegado para usuario/cliente inactivo o bloqueado: {}", correo);
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Usuario o cliente inactivo o bloqueado\","
                                    + "\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"data\":null}");
                            return;
                        }
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        correo, null, Collections.emptyList());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error validando token JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Token inválido o expirado\","
                    + "\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"data\":null}");
        }
    }
}
