# Arquitectura y Manual Operativo: Integración Financiera Gestopago

Este documento constituye la referencia técnica, arquitectónica y operativa del módulo de procesamiento de pagos de servicios y recargas electrónicas integradas con el proveedor externo **Gestopago** (`POST /sistema/service/sendTx.do`).

---

## 1. Visión General y Principios de Diseño

El módulo bancario de transacciones financieras procesa operaciones garantizando:
1. **Contratos REST estrictos:** Los endpoints reciben sus parámetros a través de `@RequestBody` mediante DTOs validados con Bean Validation (`@Valid`).
2. **Validación en servidor contra catálogo oficial:** Antes de iniciar cualquier reserva de saldo, el backend verifica contra la caché sincronizada en MongoDB (`catalogo_productos_cache`) que el servicio exista, que el producto exista y esté activo, que pertenezca al servicio solicitado, que el distribuidor coincida con el autorizado, que el monto sea idéntico al precio oficial del catálogo, que el `upc` coincida y que la referencia cumpla el formato del producto.
3. **Autorización y propiedad de recursos (Anti-IDOR):** Toda operación financiera o consulta de transacciones valida contra el `SecurityContextHolder` (token JWT) que la cuenta bancaria pertenezca al cliente autenticado.
4. **Validación estricta de PIN y estado del cliente:** Se exige usuario y cliente activos, sin bloqueos (`loginBloqueado == false`, `cuentaBloqueada == false`), cuenta en estatus `ACTIVA` y validación de PIN con hash BCrypt, bloqueando automáticamente la operación tras 3 intentos fallidos consecutivos.
5. **Desacoplamiento de I/O externo y Base de Datos:** Las transacciones de base de datos se dividen en fases cortas (`Propagation.REQUIRES_NEW`) para liberar el bloqueo pesimista (`SELECT ... FOR UPDATE`) antes de invocar por HTTP a Gestopago.
6. **Idempotencia integral y máquina de estados:** Cada solicitud exige un `idempotencyKey` único (`UNIQUE` en PostgreSQL) y se protege mediante control de concurrencia en memoria y en base de datos para todos los estados (`INICIADA`, `EN_PROCESO`, `EXITOSA`, `RECHAZADA`, `TIMEOUT`, `REVERTIDA`).

---

## 2. Flujo Financiero, Máquina de Estados e Idempotencia

### 2.1 Reglas de Idempotencia
- **`EXITOSA`:** Devuelve el resultado original (HTTP 200) sin ejecutar otro cobro ni descontar saldo.
- **`RECHAZADA`:** Devuelve el resultado original (HTTP 422) sin volver a enviar el cobro a Gestopago y sin reembolsar saldo por segunda vez.
- **`TIMEOUT`:** Conserva el estado incierto en custodia (HTTP 504) sin reenviar automáticamente el cobro ni liberar el saldo sin conciliación previa.
- **`EN_PROCESO` / `INICIADA`:** Rechaza peticiones duplicadas o simultáneas con HTTP 409 (`TRANSACCION_EN_PROCESO`).
- **`REVERTIDA`:** Impide reutilizar la clave como una operación nueva con HTTP 409 (`TRANSACCION_REVERTIDA`).
- **Conflicto de parámetros:** Si se reutiliza un `idempotencyKey` existente con diferente cuenta, monto, servicio, producto o referencia, se rechaza con HTTP 409 (`IDEMPOTENCIA_CONFLICTO_DATOS`).

### 2.2 Máquina de Estados Controlada
Las transiciones válidas están gobernadas por `EstadoTransaccion`:
- `INICIADA` -> `EN_PROCESO`, `RECHAZADA`, `REVERTIDA`
- `EN_PROCESO` -> `EXITOSA`, `RECHAZADA`, `TIMEOUT`, `REVERTIDA`
- `TIMEOUT` -> `EXITOSA`, `REVERTIDA` (exclusivamente mediante conciliación)
- Estados terminales (`EXITOSA`, `RECHAZADA`, `REVERTIDA`): no admiten transiciones posteriores ni reliquidaciones.

### 2.3 Recuperación ante Caídas entre Fases y Conciliación
- **Caída previa al envío HTTP (`RESERVADA_PRE_ENVIO`):** Si el proceso se interrumpe tras reservar el saldo pero antes de enviar la petición a Gestopago, `recuperarTransaccionesPendientes` restituye el saldo a la cuenta bancaria y marca la transacción como `REVERTIDA`.
- **Caída posterior al envío HTTP (`ENVIADA_PROVEEDOR`) o `SocketTimeoutException`:** Al ser incierto si el proveedor procesó el cobro, la transacción pasa a estado `TIMEOUT` sin devolver el saldo automáticamente. Una vez verificado el resultado con el proveedor, `conciliarTransaccionEnCustodia` consolida a `EXITOSA` o restituye el saldo pasando a `REVERTIDA`.

---

## 3. Matriz Unificada de Variables de Entorno

| Variable | Propósito | Ejemplo Ficticio | Obligatoria en Prod |
| :--- | :--- | :--- | :--- |
| `DB_URL` | Cadena JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/usuarios_db` | Sí |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres_user_ejemplo` | Sí |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `contrasena_segura_ejemplo` | Sí |
| `MONGODB_URI` | URI de conexión a MongoDB (caché catálogo) | `mongodb://localhost:27017/catalogo_cache` | Sí |
| `MONGODB_DATABASE` | Nombre de base de datos en MongoDB | `catalogo_cache` | Sí (`catalogo_cache`) |
| `JWT_SECRET` | Clave HMAC-SHA256 (>= 32 bytes) | `clave_secreta_jwt_ficticia_de_al_menos_32_caracteres_12345` | Sí |
| `JWT_EXPIRATION` | Expiración del JWT en ms | `86400000` | No (`86400000`) |
| `CORS_ALLOWED_ORIGINS` | Lista de orígenes CORS permitidos separados por coma | `http://localhost:3000,http://localhost:8080` | Sí |
| `H2_CONSOLE_ENABLED` | Habilita `/h2-console/**` solo en desarrollo local | `false` | No (`false`) |
| `GESTOPAGO_AUTH_URL` | URL base de autenticación Gestopago | `https://gestopago.portalventas.net` | Sí |
| `GESTOPAGO_DISTRIBUIDOR_ID` | ID numérico del distribuidor en Gestopago | `999` | Sí |
| `GESTOPAGO_DISPOSITIVO_CODIGO` | Código de dispositivo asignado por Gestopago | `DEV-TPV-EJEMPLO` | Sí |
| `GESTOPAGO_DISPOSITIVO_PASSWORD` | Contraseña del dispositivo en Gestopago | `contrasena_gestopago_ejemplo` | Sí |
| `GESTOPAGO_PRODUCTOS_URL` | URL base del catálogo de productos | `https://gestopago.portalventas.net` | Sí |
| `GESTOPAGO_PRODUCTOS_RUTA` | Ruta del endpoint de catálogo | `/sistema/service/getProductList.do` | No |
| `GESTOPAGO_TX_URL` | URL base para envío de transacciones | `https://gestopago.portalventas.net` | Sí |
| `GESTOPAGO_TX_RUTA` | Ruta del endpoint de transacciones | `/sistema/service/sendTx.do` | No |
| `GESTOPAGO_AES_KEY` | Clave secreta AES (>= 16 caracteres, sin default inseguro) | `clave_aes_personalizada_segura_2026` | Sí |
| `GESTOPAGO_AES_IV` | Vector de inicialización AES/CBC (exactamente 16 bytes) | `IV_FICTICIO_16BY` | Sí |
| `GESTOPAGO_AES_TRANSFORMATION` | Transformación criptográfica | `AES/CBC/PKCS5Padding` | No |

---

## 4. Instalación Local, Migraciones y Ejecución de Pruebas

1. Copiar `.env.example` a `.env` y ajustar las variables locales.
2. Las migraciones Flyway (`V0` a `V10` en `src/main/resources/db/migration/`) se ejecutan automáticamente al iniciar la aplicación sobre PostgreSQL.
3. Para ejecutar todas las pruebas automatizadas (unitarias, concurrencia e integración E2E con mocks locales):
   ```powershell
   .\gradlew.bat clean test
   ```
4. Para generar el artefacto ejecutable de producción:
   ```powershell
   .\gradlew.bat bootJar
   ```

---

## 5. Limitaciones Pendientes de Documentación Externa y Rotación de Secretos

### 5.1 Especificación Oficial de `sendTx.do` (Pendiente del Proveedor)
Antes de habilitar cobros reales en producción contra Gestopago, se debe confirmar con el manual técnico oficial del proveedor:
1. Orden exacto y separador de los campos en la cadena canónica de `firma`.
2. Mecanismo oficial de derivación de la clave AES-256 y entrega/generación del IV de 16 bytes.
3. Formato de transporte esperado por `sendTx.do` (`application/json` vs parámetros form/XML) y catálogo oficial de códigos de respuesta.
4. Endpoint oficial de consulta de estatus de transacciones para automatizar la conciliación de operaciones en estado `TIMEOUT`.

### 5.2 Rotación Obligatoria de Credenciales Históricas
Debido a que en commits anteriores a `ffc55df` se versionaron credenciales en `application.properties`, el administrador de infraestructura debe rotar en los sistemas externos antes de salir a producción:
1. Contraseña de la base de datos PostgreSQL en Supabase.
2. Usuarios/contraseñas de los clústeres de MongoDB Atlas.
3. Contraseña del dispositivo comercial en el portal de Gestopago (`GESTOPAGO_DISPOSITIVO_PASSWORD`).
4. Secreto de firma JWT (`JWT_SECRET`) y clave `GESTOPAGO_AES_KEY`.
