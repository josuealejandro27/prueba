-- ==============================================================================
-- Esquema consolidado para pruebas de integración sobre H2 (PostgreSQL Mode)
-- ==============================================================================

-- 1. Tokens de Gestopago
CREATE TABLE IF NOT EXISTS gestopago_tokens (
    id                  SERIAL PRIMARY KEY,
    id_distribuidor     INTEGER         NOT NULL,
    codigo_dispositivo  VARCHAR(100)    NOT NULL,
    token               TEXT            NOT NULL,
    token_type          VARCHAR(50),
    expires_in          BIGINT,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_gestopago_tokens UNIQUE (id_distribuidor, codigo_dispositivo)
);

-- 2. Catálogos base
CREATE TABLE IF NOT EXISTS catalogo_genero (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS catalogo_pais (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS catalogo_estado_civil (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

INSERT INTO catalogo_genero (tipo) VALUES ('Masculino'), ('Femenino'), ('Otro');
INSERT INTO catalogo_pais (nombre) VALUES ('México'), ('Estados Unidos'), ('Canadá');
INSERT INTO catalogo_estado_civil (nombre) VALUES ('Soltero'), ('Casado'), ('Divorciado'), ('Viudo');

-- 3. Entidad Persona Física (Cliente)
CREATE TABLE IF NOT EXISTS persona_fisica (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp VARCHAR(18) NOT NULL UNIQUE,
    rfc VARCHAR(13) NOT NULL UNIQUE,
    genero_id BIGINT NOT NULL,
    nacionalidad_id BIGINT NOT NULL,
    estado_civil_id BIGINT NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    lada SMALLINT NOT NULL,
    numero_telefono BIGINT NOT NULL,
    numero_telefono2 BIGINT,
    cuenta_bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    login_bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_genero FOREIGN KEY (genero_id) REFERENCES catalogo_genero(id),
    CONSTRAINT fk_nacionalidad FOREIGN KEY (nacionalidad_id) REFERENCES catalogo_pais(id),
    CONSTRAINT fk_estado_civil FOREIGN KEY (estado_civil_id) REFERENCES catalogo_estado_civil(id)
);

-- 4. Domicilio
CREATE TABLE IF NOT EXISTS domicilio (
    id BIGSERIAL PRIMARY KEY,
    persona_fisica_id BIGINT NOT NULL UNIQUE,
    calle VARCHAR(100) NOT NULL,
    no_exterior SMALLINT NOT NULL,
    no_interior SMALLINT,
    colonia VARCHAR(100) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    estado VARCHAR(100) NOT NULL,
    cp INTEGER NOT NULL,
    pais VARCHAR(100) NOT NULL,
    CONSTRAINT fk_persona_fisica_domicilio FOREIGN KEY (persona_fisica_id) REFERENCES persona_fisica(id)
);

-- 5. Cuenta Bancaria (1:N)
CREATE TABLE IF NOT EXISTS cuenta_bancaria (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(16) NOT NULL UNIQUE,
    saldo DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    persona_fisica_id BIGINT NOT NULL,
    estatus VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT fk_persona_fisica_cuenta FOREIGN KEY (persona_fisica_id) REFERENCES persona_fisica(id),
    CONSTRAINT chk_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_estatus CHECK (estatus IN ('ACTIVA', 'BLOQUEADA', 'CANCELADA'))
);

-- 6. Información Laboral
CREATE TABLE IF NOT EXISTS informacion_laboral (
    id BIGSERIAL PRIMARY KEY,
    persona_fisica_id BIGINT NOT NULL UNIQUE,
    ocupacion VARCHAR(100) NOT NULL,
    empresa VARCHAR(250) NOT NULL,
    ingreso_mensual DECIMAL(8, 2) NOT NULL,
    numero_telefono BIGINT NOT NULL,
    CONSTRAINT fk_persona_fisica_laboral FOREIGN KEY (persona_fisica_id) REFERENCES persona_fisica(id),
    CONSTRAINT chk_ingreso_positivo CHECK (ingreso_mensual > 0)
);

-- 7. Usuario
CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL UNIQUE,
    correo VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES persona_fisica(id)
);

-- 8. Catálogos Geográficos
CREATE TABLE IF NOT EXISTS catalogo_estado (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    pais_id BIGINT NOT NULL,
    CONSTRAINT fk_catalogo_estado_pais FOREIGN KEY (pais_id) REFERENCES catalogo_pais(id)
);

CREATE TABLE IF NOT EXISTS catalogo_municipio (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    estado_id BIGINT NOT NULL,
    CONSTRAINT fk_catalogo_municipio_estado FOREIGN KEY (estado_id) REFERENCES catalogo_estado(id)
);

CREATE TABLE IF NOT EXISTS catalogo_colonia (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    municipio_id BIGINT NOT NULL,
    CONSTRAINT fk_catalogo_colonia_municipio FOREIGN KEY (municipio_id) REFERENCES catalogo_municipio(id)
);

CREATE TABLE IF NOT EXISTS catalogo_nacionalidad (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

INSERT INTO catalogo_estado (nombre, pais_id) VALUES ('Ciudad de México', 1), ('Jalisco', 1);
INSERT INTO catalogo_municipio (nombre, estado_id) VALUES ('Cuauhtémoc', 1), ('Guadalajara', 2);
INSERT INTO catalogo_colonia (nombre, municipio_id) VALUES ('Centro', 1);

-- 9. Personas (tabla auxiliar)
CREATE TABLE IF NOT EXISTS personas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL
);

