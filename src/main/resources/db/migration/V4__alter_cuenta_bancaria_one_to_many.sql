-- Eliminar la restricción UNIQUE de persona_fisica_id en cuenta_bancaria
-- para permitir la relación 1:N entre Cliente y Cuenta
ALTER TABLE cuenta_bancaria DROP CONSTRAINT IF EXISTS cuenta_bancaria_persona_fisica_id_key;

-- Crear índice para mejorar las búsquedas por persona_fisica_id
CREATE INDEX IF NOT EXISTS idx_cuenta_bancaria_persona_fisica_id ON cuenta_bancaria(persona_fisica_id);
