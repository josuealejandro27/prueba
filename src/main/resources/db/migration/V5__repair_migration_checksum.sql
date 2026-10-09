-- Esta migración repara el checksum de V1 que fue modificada
-- Los cambios ya fueron aplicados en V1, solo necesitamos actualizar el checksum

-- No se necesitan cambios adicionales, solo repair del checksum
-- Ejecutar: ./gradlew flywayRepair

-- Si no se puede ejecutar flywayRepair, ejecutar manualmente en PostgreSQL:
-- UPDATE flyway_schema_history SET checksum = 700106466 WHERE version = '1';
