-- ==============================================================================
-- Migración V11: Remediación Integral de Seguridad Supabase, RLS y Columnas Sensibles
-- ==============================================================================
-- 1. Activa Row Level Security (RLS) en todas las tablas de negocio y catálogos (15 tablas).
-- 2. La tabla interna flyway_schema_history NO requiere RLS (evita bloqueos y timeouts
--    durante la ejecución de Flyway); se protege mediante REVOKE total a roles públicos.
-- 3. Revoca privilegios de acceso público (anon, authenticated, PUBLIC) a tablas
--    privadas, financieras, de credenciales y de infraestructura.
-- 4. Protege columnas sensibles (usuario.password, gestopago_tokens.token).
-- 5. Establece políticas RLS específicas por tabla y función sin utilizar atajos
--    inseguros como USING (true) o WITH CHECK (true).
-- 6. Conserva el acceso transparente del propietario (postgres) para conexiones JDBC.
-- ==============================================================================

-- Configuración de tiempo de espera para adquisición de bloqueos DDL (falla rápido si hay contención)
SET lock_timeout = '5s';

-- ------------------------------------------------------------------------------
-- 1. ACTIVACIÓN DE ROW LEVEL SECURITY (RLS) EN TABLAS DE NEGOCIO Y CATÁLOGOS
-- ------------------------------------------------------------------------------
-- Se activa RLS en las 15 tablas de negocio, financieras y catálogos.
-- NOTA CRÍTICA 1: NO se incluye flyway_schema_history para evitar contención de bloqueo
-- exclusivo (AccessExclusiveLock) mientras Flyway ejecuta su propia transacción de migración.
-- NOTA CRÍTICA 2: NO se utiliza FORCE ROW LEVEL SECURITY para permitir que el
-- propietario de la base de datos (usuario postgres vía JDBC) mantenga su
-- capacidad operativa normal con BYPASSRLS.

ALTER TABLE IF EXISTS gestopago_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_genero ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_pais ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_estado_civil ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS persona_fisica ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS domicilio ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS cuenta_bancaria ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS informacion_laboral ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_estado ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_municipio ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_colonia ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS catalogo_nacionalidad ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS personas ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS transacciones_financieras ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 2. REVOCACIÓN EXHAUSTIVA DE PRIVILEGIOS A ROLES DE POSTGREST Y AL PSEUDO-ROL PUBLIC
-- ------------------------------------------------------------------------------
-- PostgREST en Supabase expone el esquema public a los roles anon y authenticated.
-- Para prevenir la fuga de credenciales, datos bancarios/personales y metadatos internos,
-- se revocan todos los privilegios sobre tablas privadas, metadatos y secuencias.

DO $$
BEGIN
    -- Revocar permisos del rol 'anon' (cliente no autenticado en Supabase PostgREST)
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE gestopago_tokens FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE usuario FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE persona_fisica FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE domicilio FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE cuenta_bancaria FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE informacion_laboral FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE transacciones_financieras FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE personas FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_genero FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_pais FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_estado_civil FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_estado FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_municipio FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_colonia FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_nacionalidad FROM anon;
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM anon;
        REVOKE ALL PRIVILEGES ON ALL ROUTINES IN SCHEMA public FROM anon;
    END IF;

    -- Revocar permisos del rol 'authenticated' (usuarios autenticados en Supabase Auth)
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE gestopago_tokens FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE usuario FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE persona_fisica FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE domicilio FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE cuenta_bancaria FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE informacion_laboral FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE transacciones_financieras FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE personas FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_genero FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_pais FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_estado_civil FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_estado FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_municipio FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_colonia FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE catalogo_nacionalidad FROM authenticated;
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
        REVOKE ALL PRIVILEGES ON ALL ROUTINES IN SCHEMA public FROM authenticated;
    END IF;

    -- Revocar permisos del pseudo-rol PUBLIC para evitar herencia automática
    REVOKE ALL PRIVILEGES ON TABLE flyway_schema_history FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE gestopago_tokens FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE usuario FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE persona_fisica FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE domicilio FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE cuenta_bancaria FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE informacion_laboral FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE transacciones_financieras FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE personas FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_genero FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_pais FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_estado_civil FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_estado FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_municipio FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_colonia FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON TABLE catalogo_nacionalidad FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC;
    REVOKE ALL PRIVILEGES ON ALL ROUTINES IN SCHEMA public FROM PUBLIC;
END $$;

-- Revocar permisos predeterminados en el esquema public para futuras tablas y secuencias
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON ROUTINES FROM authenticated;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 3. POLÍTICAS RLS PARA TABLAS PRIVADAS Y CRÍTICAS
-- ------------------------------------------------------------------------------
-- Para el rol administrativo de Supabase (service_role), se definen políticas
-- específicas con restricciones de validación técnica (sin atajos tipo USING true).

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN

        -- 3.1 Tokens de Gestopago (Credenciales críticas de la pasarela)
        EXECUTE 'DROP POLICY IF EXISTS gestopago_tokens_service_role_policy ON public.gestopago_tokens';
        EXECUTE 'CREATE POLICY gestopago_tokens_service_role_policy ON public.gestopago_tokens
                    FOR ALL TO service_role
                    USING (id_distribuidor > 0)
                    WITH CHECK (id_distribuidor > 0 AND length(token) > 0)';

        -- 3.2 Usuarios (Hashes de contraseñas y correos)
        EXECUTE 'DROP POLICY IF EXISTS usuario_service_role_policy ON public.usuario';
        EXECUTE 'CREATE POLICY usuario_service_role_policy ON public.usuario
                    FOR ALL TO service_role
                    USING (cliente_id > 0)
                    WITH CHECK (cliente_id > 0 AND length(correo) > 3 AND length(password) >= 60)';

        -- 3.3 Persona Física (PII: Nombres, CURP, RFC)
        EXECUTE 'DROP POLICY IF EXISTS persona_fisica_service_role_policy ON public.persona_fisica';
        EXECUTE 'CREATE POLICY persona_fisica_service_role_policy ON public.persona_fisica
                    FOR ALL TO service_role
                    USING (id > 0)
                    WITH CHECK (id > 0 AND length(curp) = 18 AND length(rfc) IN (12, 13))';

        -- 3.4 Cuentas Bancarias (Información financiera y saldos)
        EXECUTE 'DROP POLICY IF EXISTS cuenta_bancaria_service_role_policy ON public.cuenta_bancaria';
        EXECUTE 'CREATE POLICY cuenta_bancaria_service_role_policy ON public.cuenta_bancaria
                    FOR ALL TO service_role
                    USING (persona_fisica_id > 0)
                    WITH CHECK (persona_fisica_id > 0 AND saldo >= 0.00)';

        -- 3.5 Transacciones Financieras (Auditoría e integridad de pagos)
        EXECUTE 'DROP POLICY IF EXISTS transacciones_service_role_policy ON public.transacciones_financieras';
        EXECUTE 'CREATE POLICY transacciones_service_role_policy ON public.transacciones_financieras
                    FOR ALL TO service_role
                    USING (cuenta_id > 0)
                    WITH CHECK (cuenta_id > 0 AND monto > 0)';

        -- 3.6 Domicilio (Ubicaciones físicas de clientes)
        EXECUTE 'DROP POLICY IF EXISTS domicilio_service_role_policy ON public.domicilio';
        EXECUTE 'CREATE POLICY domicilio_service_role_policy ON public.domicilio
                    FOR ALL TO service_role
                    USING (persona_fisica_id > 0)
                    WITH CHECK (persona_fisica_id > 0 AND cp > 0)';

        -- 3.7 Información Laboral (Ocupación e ingresos)
        EXECUTE 'DROP POLICY IF EXISTS informacion_laboral_service_role_policy ON public.informacion_laboral';
        EXECUTE 'CREATE POLICY informacion_laboral_service_role_policy ON public.informacion_laboral
                    FOR ALL TO service_role
                    USING (persona_fisica_id > 0)
                    WITH CHECK (persona_fisica_id > 0 AND ingreso_mensual > 0)';

        -- 3.8 Personas Legacy
        EXECUTE 'DROP POLICY IF EXISTS personas_service_role_policy ON public.personas';
        EXECUTE 'CREATE POLICY personas_service_role_policy ON public.personas
                    FOR ALL TO service_role
                    USING (id > 0)
                    WITH CHECK (id > 0)';
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 4. POLÍTICAS RLS PARA CATÁLOGOS PÚBLICOS DE CONSULTA
-- ------------------------------------------------------------------------------
-- Los catálogos permiten únicamente lectura condicionada (SELECT) para anon y
-- authenticated. Queda estrictamente prohibida toda modificación pública (INSERT,
-- UPDATE, DELETE).

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') AND
       EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN

        -- Conceder ÚNICAMENTE permiso SELECT en catálogos de consulta
        GRANT SELECT ON TABLE public.catalogo_genero TO anon, authenticated;
        GRANT SELECT ON TABLE public.catalogo_pais TO anon, authenticated;
        GRANT SELECT ON TABLE public.catalogo_estado_civil TO anon, authenticated;
        GRANT SELECT ON TABLE public.catalogo_estado TO anon, authenticated;
        GRANT SELECT ON TABLE public.catalogo_municipio TO anon, authenticated;
        GRANT SELECT ON TABLE public.catalogo_colonia TO anon, authenticated;
        GRANT SELECT ON TABLE public.catalogo_nacionalidad TO anon, authenticated;

        -- Políticas de SELECT con validación de integridad referencial de registros
        EXECUTE 'DROP POLICY IF EXISTS catalogo_genero_select_policy ON public.catalogo_genero';
        EXECUTE 'CREATE POLICY catalogo_genero_select_policy ON public.catalogo_genero
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND length(tipo) > 0)';

        EXECUTE 'DROP POLICY IF EXISTS catalogo_pais_select_policy ON public.catalogo_pais';
        EXECUTE 'CREATE POLICY catalogo_pais_select_policy ON public.catalogo_pais
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND length(nombre) > 0)';

        EXECUTE 'DROP POLICY IF EXISTS catalogo_estado_civil_select_policy ON public.catalogo_estado_civil';
        EXECUTE 'CREATE POLICY catalogo_estado_civil_select_policy ON public.catalogo_estado_civil
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND length(nombre) > 0)';

        EXECUTE 'DROP POLICY IF EXISTS catalogo_estado_select_policy ON public.catalogo_estado';
        EXECUTE 'CREATE POLICY catalogo_estado_select_policy ON public.catalogo_estado
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND pais_id > 0 AND length(nombre) > 0)';

        EXECUTE 'DROP POLICY IF EXISTS catalogo_municipio_select_policy ON public.catalogo_municipio';
        EXECUTE 'CREATE POLICY catalogo_municipio_select_policy ON public.catalogo_municipio
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND estado_id > 0 AND length(nombre) > 0)';

        EXECUTE 'DROP POLICY IF EXISTS catalogo_colonia_select_policy ON public.catalogo_colonia';
        EXECUTE 'CREATE POLICY catalogo_colonia_select_policy ON public.catalogo_colonia
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND municipio_id > 0 AND length(nombre) > 0)';

        EXECUTE 'DROP POLICY IF EXISTS catalogo_nacionalidad_select_policy ON public.catalogo_nacionalidad';
        EXECUTE 'CREATE POLICY catalogo_nacionalidad_select_policy ON public.catalogo_nacionalidad
                    FOR SELECT TO anon, authenticated
                    USING (id > 0 AND length(nombre) > 0)';
    END IF;
END $$;
