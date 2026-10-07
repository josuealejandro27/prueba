-- Tabla de Catálogo de Géneros
CREATE TABLE IF NOT EXISTS catalogo_genero (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL
);

-- Tabla de Catálogo de Países
CREATE TABLE IF NOT EXISTS catalogo_pais (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- Tabla de Catálogo de Estado Civil
CREATE TABLE IF NOT EXISTS catalogo_estado_civil (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- Insertar datos iniciales de catálogos
INSERT INTO catalogo_genero (tipo) VALUES ('Masculino'), ('Femenino'), ('Otro');
INSERT INTO catalogo_pais (nombre) VALUES ('México'), ('Estados Unidos'), ('Canadá');
INSERT INTO catalogo_estado_civil (nombre) VALUES ('Soltero'), ('Casado'), ('Divorciado'), ('Viudo');
