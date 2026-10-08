package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger UI).
 * Define el servidor local y el esquema de seguridad Bearer JWT para poder
 * probar los endpoints protegidos desde la interfaz.
 */
@Configuration
public class OpenApi {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8080").description("Local"))
                .info(new Info()
                        .title("API Sistema Bancario")
                        .description("Gestión de clientes, cuentas bancarias y usuarios de acceso")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
