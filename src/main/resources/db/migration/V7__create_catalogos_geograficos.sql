-- Catálogo de Estados (entidades federativas) relacionados con catálogo_pais
CREATE TABLE IF NOT EXISTS catalogo_estado (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    pais_id BIGINT NOT NULL,
    CONSTRAINT fk_catalogo_estado_pais FOREIGN KEY (pais_id) REFERENCES catalogo_pais(id)
);
CREATE INDEX IF NOT EXISTS idx_catalogo_estado_pais ON catalogo_estado(pais_id);
CREATE INDEX IF NOT EXISTS idx_catalogo_estado_nombre ON catalogo_estado(lower(nombre));

-- Catálogo de Municipios relacionados con catálogo_estado
CREATE TABLE IF NOT EXISTS catalogo_municipio (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    estado_id BIGINT NOT NULL,
    CONSTRAINT fk_catalogo_municipio_estado FOREIGN KEY (estado_id) REFERENCES catalogo_estado(id)
);
CREATE INDEX IF NOT EXISTS idx_catalogo_municipio_estado ON catalogo_municipio(estado_id);
CREATE INDEX IF NOT EXISTS idx_catalogo_municipio_nombre ON catalogo_municipio(lower(nombre));

-- Catálogo de Colonias relacionados con catálogo_municipio
CREATE TABLE IF NOT EXISTS catalogo_colonia (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    municipio_id BIGINT NOT NULL,
    CONSTRAINT fk_catalogo_colonia_municipio FOREIGN KEY (municipio_id) REFERENCES catalogo_municipio(id)
);
CREATE INDEX IF NOT EXISTS idx_catalogo_colonia_municipio ON catalogo_colonia(municipio_id);
CREATE INDEX IF NOT EXISTS idx_catalogo_colonia_nombre ON catalogo_colonia(lower(nombre));

-- Catálogo de Nacionalidades
CREATE TABLE IF NOT EXISTS catalogo_nacionalidad (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_catalogo_nacionalidad_nombre ON catalogo_nacionalidad(lower(nombre));

-- Datos semilla: 32 entidades federativas de México (pais_id 1 = México)
INSERT INTO catalogo_estado (nombre, pais_id) VALUES
('Aguascalientes', 1), ('Baja California', 1), ('Baja California Sur', 1),
('Campeche', 1), ('Chiapas', 1), ('Chihuahua', 1), ('Coahuila', 1), ('Colima', 1),
('Ciudad de México', 1), ('Durango', 1), ('Guanajuato', 1), ('Guerrero', 1),
('Hidalgo', 1), ('Jalisco', 1), ('Estado de México', 1), ('Michoacán', 1),
('Morelos', 1), ('Nayarit', 1), ('Nuevo León', 1), ('Oaxaca', 1), ('Puebla', 1),
('Querétaro', 1), ('Quintana Roo', 1), ('San Luis Potosí', 1), ('Sinaloa', 1),
('Sonora', 1), ('Tabasco', 1), ('Tamaulipas', 1), ('Tlaxcala', 1),
('Veracruz', 1), ('Yucatán', 1), ('Zacatecas', 1)
ON CONFLICT DO NOTHING;

-- Datos semilla: municipios representativos por entidad
INSERT INTO catalogo_municipio (nombre, estado_id)
SELECT 'Álvaro Obregón', id FROM catalogo_estado WHERE nombre = 'Ciudad de México'
UNION ALL SELECT 'Benito Juárez', id FROM catalogo_estado WHERE nombre = 'Ciudad de México'
UNION ALL SELECT 'Coyoacán', id FROM catalogo_estado WHERE nombre = 'Ciudad de México'
UNION ALL SELECT 'Cuauhtémoc', id FROM catalogo_estado WHERE nombre = 'Ciudad de México'
UNION ALL SELECT 'Iztapalapa', id FROM catalogo_estado WHERE nombre = 'Ciudad de México'
UNION ALL SELECT 'Miguel Hidalgo', id FROM catalogo_estado WHERE nombre = 'Ciudad de México'
UNION ALL SELECT 'Guadalajara', id FROM catalogo_estado WHERE nombre = 'Jalisco'
UNION ALL SELECT 'Zapopan', id FROM catalogo_estado WHERE nombre = 'Jalisco'
UNION ALL SELECT 'Tlaquepaque', id FROM catalogo_estado WHERE nombre = 'Jalisco'
UNION ALL SELECT 'Monterrey', id FROM catalogo_estado WHERE nombre = 'Nuevo León'
UNION ALL SELECT 'Guadalupe', id FROM catalogo_estado WHERE nombre = 'Nuevo León'
UNION ALL SELECT 'San Pedro Garza García', id FROM catalogo_estado WHERE nombre = 'Nuevo León'
UNION ALL SELECT 'Toluca', id FROM catalogo_estado WHERE nombre = 'Estado de México'
UNION ALL SELECT 'Ecatepec', id FROM catalogo_estado WHERE nombre = 'Estado de México'
UNION ALL SELECT 'Mérida', id FROM catalogo_estado WHERE nombre = 'Yucatán'
UNION ALL SELECT 'Puebla', id FROM catalogo_estado WHERE nombre = 'Puebla'
UNION ALL SELECT 'Cancún', id FROM catalogo_estado WHERE nombre = 'Quintana Roo'
UNION ALL SELECT 'León', id FROM catalogo_estado WHERE nombre = 'Guanajuato';

-- Datos semilla: colonias por municipio
INSERT INTO catalogo_colonia (nombre, municipio_id)
SELECT 'Centro', id FROM catalogo_municipio WHERE nombre = 'Cuauhtémoc'
UNION ALL SELECT 'Roma Norte', id FROM catalogo_municipio WHERE nombre = 'Cuauhtémoc'
UNION ALL SELECT 'Condesa', id FROM catalogo_municipio WHERE nombre = 'Cuauhtémoc'
UNION ALL SELECT 'Juárez', id FROM catalogo_municipio WHERE nombre = 'Cuauhtémoc'
UNION ALL SELECT 'Doctores', id FROM catalogo_municipio WHERE nombre = 'Cuauhtémoc'
UNION ALL SELECT 'Del Valle', id FROM catalogo_municipio WHERE nombre = 'Benito Juárez'
UNION ALL SELECT 'Nápoles', id FROM catalogo_municipio WHERE nombre = 'Benito Juárez'
UNION ALL SELECT 'Narvarte', id FROM catalogo_municipio WHERE nombre = 'Benito Juárez'
UNION ALL SELECT 'Coyoacán Centro', id FROM catalogo_municipio WHERE nombre = 'Coyoacán'
UNION ALL SELECT 'Del Carmen', id FROM catalogo_municipio WHERE nombre = 'Coyoacán'
UNION ALL SELECT 'Centro', id FROM catalogo_municipio WHERE nombre = 'Guadalajara'
UNION ALL SELECT 'Chapultepec', id FROM catalogo_municipio WHERE nombre = 'Guadalajara'
UNION ALL SELECT 'Providencia', id FROM catalogo_municipio WHERE nombre = 'Zapopan'
UNION ALL SELECT 'Centro', id FROM catalogo_municipio WHERE nombre = 'Monterrey'
UNION ALL SELECT 'San Jerónimo', id FROM catalogo_municipio WHERE nombre = 'Monterrey'
UNION ALL SELECT 'Centro', id FROM catalogo_municipio WHERE nombre = 'Mérida';

-- Datos semilla: nacionalidades
INSERT INTO catalogo_nacionalidad (nombre) VALUES
('Mexicana'), ('Estadounidense'), ('Canadiense'), ('Española'), ('Colombiana'), ('Argentina')
ON CONFLICT DO NOTHING;