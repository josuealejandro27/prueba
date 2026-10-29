-- Migración V9: Creación de la tabla transacciones_financieras para persistencia y auditoría de pagos
CREATE TABLE IF NOT EXISTS transacciones_financieras (
    id BIGSERIAL PRIMARY KEY,
    folio VARCHAR(36) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    cuenta_id BIGINT NOT NULL,
    monto DECIMAL(10, 2) NOT NULL,
    tipo_operacion VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    id_distribuidor INTEGER NOT NULL,
    id_servicio BIGINT,
    id_producto BIGINT,
    referencia VARCHAR(100) NOT NULL,
    codigo_respuesta_proveedor VARCHAR(20),
    mensaje_proveedor TEXT,
    folio_proveedor VARCHAR(100),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaccion_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_bancaria(id),
    CONSTRAINT chk_monto_positivo CHECK (monto > 0),
    CONSTRAINT chk_estado_transaccion CHECK (estado IN ('INICIADA', 'EN_PROCESO', 'EXITOSA', 'RECHAZADA', 'TIMEOUT', 'REVERTIDA'))
);

CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta ON transacciones_financieras(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_estado ON transacciones_financieras(estado);
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha ON transacciones_financieras(fecha_creacion);

