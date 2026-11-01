package com.proyecto.servicios.security;

import com.proyecto.servicios.config.ConfigDB;
import com.proyecto.servicios.config.ProductionEnvironmentValidator;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas de Seguridad Integral Supabase, RLS y Protección de Columnas Sensibles")
class SupabaseSecurityAndRlsTest {

    private static final List<String> TABLAS_CRITICAS_AUDITADAS = List.of(
            "gestopago_tokens",
            "catalogo_genero",
            "catalogo_pais",
            "catalogo_estado_civil",
            "persona_fisica",
            "domicilio",
            "cuenta_bancaria",
            "usuario",
            "informacion_laboral",
            "catalogo_estado",
            "catalogo_municipio",
            "catalogo_colonia",
            "catalogo_nacionalidad",
            "personas",
            "transacciones_financieras"
    );

    @Test
    @DisplayName("V11 activa Row Level Security (RLS) en todas las 15 tablas de negocio y catálogos")
    void verificarMigracionV11ActivaRlsEnTodasLasTablas() throws IOException {
        Path migrationPath = Path.of("src/main/resources/db/migration/V11__supabase_rls_y_seguridad.sql");
        assertThat(migrationPath).exists();

        String rawSql = Files.readString(migrationPath, StandardCharsets.UTF_8);
        String sqlWithoutComments = rawSql.replaceAll("--.*", "").toLowerCase();

        for (String tabla : TABLAS_CRITICAS_AUDITADAS) {
            String patronEsperado = "alter table if exists " + tabla + " enable row level security;";
            assertThat(sqlWithoutComments)
                    .withFailMessage("La tabla " + tabla + " debe tener ENABLE ROW LEVEL SECURITY en V11")
                    .contains(patronEsperado);
        }

        // Verificar que NO se aplica la sentencia DDL FORCE ROW LEVEL SECURITY
        assertThat(sqlWithoutComments).doesNotContain("force row level security");
    }

    @Test
    @DisplayName("flyway_schema_history se protege mediante REVOKE total sin RLS para evitar contención y timeouts")
    void verificarFlywaySchemaHistoryProtegidaSinRls() throws IOException {
        Path migrationPath = Path.of("src/main/resources/db/migration/V11__supabase_rls_y_seguridad.sql");
        String rawSql = Files.readString(migrationPath, StandardCharsets.UTF_8);
        String sqlWithoutComments = rawSql.replaceAll("--.*", "").toLowerCase();

        // NO debe tener ENABLE ROW LEVEL SECURITY (causaría self-deadlock / statement timeout en Flyway)
        assertThat(sqlWithoutComments).doesNotContain("flyway_schema_history enable row level security");

        // DEBE tener revocación total de privilegios a roles públicos
        assertThat(rawSql).contains("REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM anon;");
        assertThat(rawSql).contains("REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM authenticated;");
        assertThat(rawSql).contains("REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM PUBLIC;");
    }

    @Test
    @DisplayName("V11 NO contiene políticas abiertas inseguras con USING (true) o WITH CHECK (true)")
    void verificarMigracionV11NoContieneUsingTrueGeneral() throws IOException {
        Path migrationPath = Path.of("src/main/resources/db/migration/V11__supabase_rls_y_seguridad.sql");
        String rawSql = Files.readString(migrationPath, StandardCharsets.UTF_8);
        String sqlWithoutComments = rawSql.replaceAll("--.*", "");

        // Ninguna política debe tener USING (true) o WITH CHECK (true) abierto a anon o public
        assertThat(sqlWithoutComments).doesNotContain("TO anon USING (true)");
        assertThat(sqlWithoutComments).doesNotContain("TO anon, authenticated USING (true)");
        assertThat(sqlWithoutComments).doesNotContain("TO public USING (true)");
        assertThat(sqlWithoutComments).doesNotContain("WITH CHECK (true)");
    }

    @Test
    @DisplayName("V11 revoca privilegios a roles anon, authenticated y PUBLIC sobre tablas privadas")
    void verificarMigracionV11RevocaPrivilegiosPublicos() throws IOException {
        Path migrationPath = Path.of("src/main/resources/db/migration/V11__supabase_rls_y_seguridad.sql");
        String sql = Files.readString(migrationPath, StandardCharsets.UTF_8);

        // Tablas que deben tener revocación explícita
        List<String> tablasPrivadas = List.of(
                "flyway_schema_history",
                "gestopago_tokens",
                "usuario",
                "persona_fisica",
                "domicilio",
                "cuenta_bancaria",
                "informacion_laboral",
                "transacciones_financieras",
                "personas"
        );

        for (String tabla : tablasPrivadas) {
            assertThat(sql).contains("REVOKE ALL PRIVILEGES ON TABLE " + tabla + " FROM anon;");
            assertThat(sql).contains("REVOKE ALL PRIVILEGES ON TABLE " + tabla + " FROM authenticated;");
            assertThat(sql).contains("REVOKE ALL PRIVILEGES ON TABLE " + tabla + " FROM PUBLIC;");
        }

        // Revocación de privilegios predeterminados
        assertThat(sql).contains("ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM PUBLIC;");
        assertThat(sql).contains("ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM PUBLIC;");
    }

    @Test
    @DisplayName("El DTO UsuarioResponse nunca expone el hash de contraseña al frontend")
    void verificarUsuarioResponseNoExponeCampoPassword() {
        Field[] campos = UsuarioResponse.class.getDeclaredFields();
        Set<String> nombresCampos = Arrays.stream(campos)
                .map(Field::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        assertThat(nombresCampos).doesNotContain("password");
        assertThat(nombresCampos).doesNotContain("contrasena");
        assertThat(nombresCampos).doesNotContain("pwd");
        assertThat(nombresCampos).doesNotContain("hash");
    }

    @Test
    @DisplayName("No existe ningún controlador REST público que exponga la tabla gestopago_tokens")
    void verificarGestoPagoTokenNoTieneControladorPublico() {
        File controllersDir = new File("src/main/java/com/proyecto/servicios/controller");
        assertThat(controllersDir).exists();

        File[] files = controllersDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertThat(files).isNotNull();

        for (File f : files) {
            assertThat(f.getName().toLowerCase())
                    .withFailMessage("No debe existir controlador para exponer tokens de Gestopago: " + f.getName())
                    .doesNotContain("token");
        }
    }

    @Test
    @DisplayName("ConfigDB normaliza URLs de Supabase y respeta SUPABASE_DB_PASSWORD")
    void verificarConfigDbNormalizaUrlsYExtraeCredenciales() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SUPABASE_DB_HOST", "db.odwnbycqnyxaohqnyjbv.supabase.co");
        env.setProperty("SUPABASE_DB_PORT", "5432");
        env.setProperty("SUPABASE_DB_NAME", "postgres");
        env.setProperty("SUPABASE_DB_USERNAME", "postgres");
        env.setProperty("SUPABASE_DB_PASSWORD", "Josueymario2715");

        ConfigDB configDB = new ConfigDB(env);

        Method procesarMethod = ConfigDB.class.getDeclaredMethod("procesarCredencialesYUrl", String.class, String.class, String.class);
        procesarMethod.setAccessible(true);

        // Caso 1: URL estándar
        String[] res1 = (String[]) procesarMethod.invoke(configDB,
                "jdbc:postgresql://db.odwnbycqnyxaohqnyjbv.supabase.co:5432/postgres", "postgres", "Josueymario2715");
        assertThat(res1[0]).isEqualTo("jdbc:postgresql://db.odwnbycqnyxaohqnyjbv.supabase.co:5432/postgres");
        assertThat(res1[1]).isEqualTo("postgres");
        assertThat(res1[2]).isEqualTo("Josueymario2715");

        // Caso 2: URL de Supabase copiada con protocolo postgresql://
        String[] res2 = (String[]) procesarMethod.invoke(configDB,
                "postgresql://db.odwnbycqnyxaohqnyjbv.supabase.co:5432/postgres", "postgres", "Josueymario2715");
        assertThat(res2[0]).isEqualTo("jdbc:postgresql://db.odwnbycqnyxaohqnyjbv.supabase.co:5432/postgres");

        // Caso 3: URL con credenciales embebidas (ej. formato Render/Heroku)
        String[] res3 = (String[]) procesarMethod.invoke(configDB,
                "postgresql://admin_user:Pass%23Secret123@db.odwnbycqnyxaohqnyjbv.supabase.co:5432/postgres", "", "");
        assertThat(res3[0]).isEqualTo("jdbc:postgresql://db.odwnbycqnyxaohqnyjbv.supabase.co:5432/postgres");
        assertThat(res3[1]).isEqualTo("admin_user");
        assertThat(res3[2]).isEqualTo("Pass%23Secret123");
    }

    @Test
    @DisplayName("ProductionEnvironmentValidator falla si faltan las variables de Supabase en producción")
    void verificarProductionEnvironmentValidatorFallaSiFaltanVariables() {
        MockEnvironment envIncompleto = new MockEnvironment();
        // Solo configuramos unas pocas variables
        envIncompleto.setProperty("jwt.secret", "clave_valida_de_al_menos_32_bytes_de_longitud!");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validarEntorno(envIncompleto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fallo crítico de configuración para producción");
    }
}

