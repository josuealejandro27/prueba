-- Migración V10: Índices compuestos para recuperación de transacciones pendientes y validación de cuentas
CREATE INDEX IF NOT EXISTS idx_transacciones_estado_actualizacion
    ON transacciones_financieras(estado, fecha_actualizacion);

CREATE INDEX IF NOT EXISTS idx_cuenta_bancaria_persona_estatus
    ON cuenta_bancaria(persona_fisica_id, estatus);

