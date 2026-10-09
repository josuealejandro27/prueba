-- Tabla legacy de personas (endpoints /personas, /personasActualiza, /personasElimina).
-- No existía en ninguna migración previa, por lo que esas rutas devolvían error 500.
CREATE TABLE IF NOT EXISTS personas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100),
    apellido_paterno VARCHAR(100),
    apellido_materno VARCHAR(100)
);
