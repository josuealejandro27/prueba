package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Público: login
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                // Público: registro de un cliente
                .requestMatchers(HttpMethod.POST, "/clientes/agregar").permitAll()
                // Público: consultas de catálogos (búsqueda y consulta por id) vía POST con DTO en el cuerpo
                .requestMatchers(HttpMethod.POST, "/catalogos/*/consultar", "/catalogos/*/buscar").permitAll()
                .requestMatchers(HttpMethod.POST, "/gestopago/catalogo", "/catalogo/productos").permitAll()
                // Documentación e infraestructura
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // Mismo envoltorio JSON que ApiResponse (code, message, timestamp, data)
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"No autenticado o token inválido\","
                            + "\"timestamp\":\"" + LocalDateTime.now() + "\",\"data\":null}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Acceso denegado\","
                            + "\"timestamp\":\"" + LocalDateTime.now() + "\",\"data\":null}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
