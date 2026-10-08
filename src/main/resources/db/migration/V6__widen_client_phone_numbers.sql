-- Los teléfonos de 10 dígitos pueden superar el rango de INTEGER (2,147,483,647).
ALTER TABLE persona_fisica
    ALTER COLUMN numero_telefono TYPE BIGINT,
    ALTER COLUMN numero_telefono2 TYPE BIGINT;

ALTER TABLE informacion_laboral
    ALTER COLUMN numero_telefono TYPE BIGINT;
