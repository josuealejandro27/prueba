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

/**
 * Configuración de la documentación OpenAPI (Swagger UI).
 *
 * No fija una URL de servidor absoluta (evita "localhost:8080" en producción):
 *  - Por defecto usa la URL relativa "/", de modo que Swagger apunta al mismo
 *    origen desde el que se sirve (localhost en desarrollo, Render en producción).
 *  - Opcionalmente se puede sobrescribir con la propiedad
 *    app.openapi.server-url (variable de entorno OPENAPI_SERVER_URL).
 */
@Configuration
public class OpenApi {

    @Value("${app.openapi.server-url:}")
    private String serverUrl;

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

        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.addServersItem(new Server().url(serverUrl).description("Servidor desplegado"));
        } else {
            openAPI.addServersItem(new Server().url("/").description("Servidor actual"));
        }
        return openAPI;
    }
}
