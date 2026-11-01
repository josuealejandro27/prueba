package com.proyecto.servicios.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador de arranque para el perfil de producción ({@code prod}).
 * Garantiza que la aplicación falle de forma clara y segura (fail-fast) si faltan
 * variables de entorno críticas de base de datos, seguridad JWT o integración Gestopago.
 */
@Slf4j
@Component
@Profile("prod")
public class ProductionEnvironmentValidator {

    private final Environment env;

    public ProductionEnvironmentValidator(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void validarVariablesObligatorias() {
        validarEntorno(env);
    }

    public static void validarEntorno(Environment env) {
        List<String> faltantesOInvalidas = new ArrayList<>();

        verificarPropiedad(env, "spring.datasource.url", "SUPABASE_DB_URL / DB_URL / SPRING_DATASOURCE_URL", faltantesOInvalidas);
        verificarPropiedad(env, "spring.datasource.username", "SUPABASE_DB_USERNAME / DB_USERNAME / SPRING_DATASOURCE_USERNAME", faltantesOInvalidas);
        verificarPropiedad(env, "spring.datasource.password", "SUPABASE_DB_PASSWORD / DB_PASSWORD / SPRING_DATASOURCE_PASSWORD", faltantesOInvalidas);
        verificarPropiedad(env, "spring.data.mongodb.uri", "MONGODB_URI", faltantesOInvalidas);
        verificarPropiedad(env, "gestopago.auth.codigo-dispositivo", "GESTOPAGO_DISPOSITIVO_CODIGO", faltantesOInvalidas);
        verificarPropiedad(env, "gestopago.auth.password", "GESTOPAGO_DISPOSITIVO_PASSWORD", faltantesOInvalidas);

        String idDist = env.getProperty("gestopago.auth.id-distribuidor");
        if (idDist == null || idDist.isBlank() || "0".equals(idDist.trim())) {
            faltantesOInvalidas.add("GESTOPAGO_DISTRIBUIDOR_ID (debe ser un entero positivo)");
        }

        String jwtSecret = env.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            faltantesOInvalidas.add("JWT_SECRET (mínimo 32 bytes / 256 bits)");
        }

        String aesKey = env.getProperty("gestopago.tx.aes.key");
        if (aesKey == null || aesKey.isBlank() || aesKey.trim().length() < 16) {
            faltantesOInvalidas.add("GESTOPAGO_AES_KEY (mínimo 16 caracteres)");
        }

        String aesIv = env.getProperty("gestopago.tx.aes.iv");
        if (aesIv == null || aesIv.getBytes(StandardCharsets.UTF_8).length != 16) {
            faltantesOInvalidas.add("GESTOPAGO_AES_IV (exactamente 16 bytes)");
        }

        if (!faltantesOInvalidas.isEmpty()) {
            String mensaje = "Fallo crítico de configuración para producción. Variables faltantes o inválidas: "
                    + String.join(", ", faltantesOInvalidas);
            log.error(mensaje);
            throw new IllegalStateException(mensaje);
        }

        log.info("Validación de variables de entorno de producción completada satisfactoriamente.");
    }

    private static void verificarPropiedad(Environment env, String propertyKey, String nombreVariable, List<String> errores) {
        String valor = env.getProperty(propertyKey);
        if (valor == null || valor.isBlank()) {
            errores.add(nombreVariable);
        }
    }
}

