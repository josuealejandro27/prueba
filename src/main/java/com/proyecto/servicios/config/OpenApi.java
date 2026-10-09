package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class OpenApi {

    private static final String RENDER_URL_DEFAULT = "https://prueba-gestopago.onrender.com";

    /**
     * URL pública del despliegue. En Render se resuelve sola desde
     * RENDER_EXTERNAL_URL; si está vacía se asume entorno local.
     */
    @Value("${app.openapi.server-url:${RENDER_EXTERNAL_URL:}}")
    private String deployedUrl;

    /** URL local de desarrollo. */
    @Value("${app.openapi.local-url:http://localhost:8080}")
    private String localUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Sistema Bancario Core - API REST")
                        .version("2.0.0")
                        .description("API REST para la administración integral del Sistema Bancario: Gestión de Clientes, Domicilios, Cuentas Bancarias, Usuarios de Acceso, Autenticación JWT y Catálogos.")
                        .contact(new Contact()
                                .name("Equipo de Ingeniería Bancaria")
                                .email("soporte@banco.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese su token JWT obtenido en el endpoint /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));

        boolean deployed = deployedUrl != null && !deployedUrl.isBlank();
        String publicUrl = deployed ? deployedUrl : RENDER_URL_DEFAULT;

        List<Server> servers = new ArrayList<>();
        if (deployed) {
            // Producción (Render): el público primero
            servers.add(new Server().url(publicUrl).description("Servidor Render"));
            servers.add(new Server().url(localUrl).description("Servidor Local de Desarrollo"));
        } else {
            // Desarrollo local: localhost primero
            servers.add(new Server().url(localUrl).description("Servidor Local de Desarrollo"));
            servers.add(new Server().url(publicUrl).description("Servidor Render"));
        }
        openAPI.setServers(servers);
        return openAPI;
    }
}
