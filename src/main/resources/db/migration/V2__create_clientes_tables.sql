-- Tabla de Clientes (Persona Física)
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
    numero_telefono INTEGER NOT NULL,
    numero_telefono2 INTEGER,
    cuenta_bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    login_bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_genero FOREIGN KEY (genero_id) REFERENCES catalogo_genero(id),
    CONSTRAINT fk_nacionalidad FOREIGN KEY (nacionalidad_id) REFERENCES catalogo_pais(id),
    CONSTRAINT fk_estado_civil FOREIGN KEY (estado_civil_id) REFERENCES catalogo_estado_civil(id)
);

-- Tabla de Domicilios
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

-- Tabla de Cuentas Bancarias
CREATE TABLE IF NOT EXISTS cuenta_bancaria (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(16) NOT NULL UNIQUE,
    saldo DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    persona_fisica_id BIGINT NOT NULL UNIQUE,
    estatus VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT fk_persona_fisica_cuenta FOREIGN KEY (persona_fisica_id) REFERENCES persona_fisica(id),
    CONSTRAINT chk_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_estatus CHECK (estatus IN ('ACTIVA', 'BLOQUEADA', 'CANCELADA'))
);

-- Tabla de Información Laboral
CREATE TABLE IF NOT EXISTS informacion_laboral (
    id BIGSERIAL PRIMARY KEY,
    persona_fisica_id BIGINT NOT NULL UNIQUE,
    ocupacion VARCHAR(100) NOT NULL,
    empresa VARCHAR(250) NOT NULL,
    ingreso_mensual DECIMAL(8, 2) NOT NULL,
    numero_telefono INTEGER NOT NULL,
    CONSTRAINT fk_persona_fisica_laboral FOREIGN KEY (persona_fisica_id) REFERENCES persona_fisica(id),
    CONSTRAINT chk_ingreso_positivo CHECK (ingreso_mensual > 0)
);

-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_persona_fisica_curp ON persona_fisica(curp);
CREATE INDEX IF NOT EXISTS idx_persona_fisica_rfc ON persona_fisica(rfc);
CREATE INDEX IF NOT EXISTS idx_persona_fisica_correo ON persona_fisica(correo);
CREATE INDEX IF NOT EXISTS idx_persona_fisica_nombre ON persona_fisica(nombre);
CREATE INDEX IF NOT EXISTS idx_persona_fisica_apellido_paterno ON persona_fisica(apellido_paterno);
CREATE INDEX IF NOT EXISTS idx_persona_fisica_apellido_materno ON persona_fisica(apellido_materno);
CREATE INDEX IF NOT EXISTS idx_persona_fisica_activo ON persona_fisica(activo);
CREATE INDEX IF NOT EXISTS idx_cuenta_bancaria_numero ON cuenta_bancaria(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuenta_bancaria_persona ON cuenta_bancaria(persona_fisica_id);
CREATE INDEX IF NOT EXISTS idx_cuenta_bancaria_estatus ON cuenta_bancaria(estatus);
