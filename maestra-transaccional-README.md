# Alquiler de Vehiculos - Backend (Salvador Quispe)


estandar de java/hct/apellido/ nombre
misma estructura

cliente alquiler
2 microservicios en Spring WebFlux + PostgreSQL (via R2DBC), JDK 26.

| | Carpeta | Puerto |
|---|---|---|
| Maestro (clientes) | `maestra/` | `8083` |
| Transaccional (alquileres) | `transaccional/` | `8084` |

## Como ejecutar

Requisitos: JDK 26, Maven. La conexion a PostgreSQL ya esta en `maestra/.env` y `transaccional/.env`.

1. Correr **maestra**:
   ```
   cd maestra
   mvn -q clean package -DskipTests
   java -jar target/maestra-1.0.0.jar
   ```
2. Correr **transaccional** (en otra terminal):
   ```
   cd transaccional
   mvn -q clean package -DskipTests
   java -jar target/transaccional-1.0.0.jar
   ```

Levantar primero `maestra`, porque `transaccional` la necesita para validar los `clienteId`.

> No uses el boton Run de VS Code sobre el `.java` (falla con "SpringApplication cannot be resolved" porque no usa el classpath de Maven). Siempre `mvn package` + `java -jar`.
> No uses `mvn spring-boot:run` (falla con JDK 26). Compila el jar y corrilo con `java -jar`.

### Con Docker (alternativa a correrlo con Maven)

Las imagenes ya estan publicadas en Docker Hub: `salvaqc/maestra:latest` y `salvaqc/transaccional:latest`.

Levantar ambos juntos con un solo comando (desde la raiz del repo, donde esta `docker-compose.yml`):
```
docker compose up -d
```
Esto usa el `.env` de la raiz (mismas credenciales de Postgres) y expone los mismos puertos que en local: `8083` (maestra) y `8084` (transaccional). Las URLs de la seccion de abajo funcionan exactamente igual, corras local con `java -jar` o con Docker — **es el mismo CRUD, las mismas URLs**, la unica diferencia es donde vive el proceso.

Para bajar los contenedores: `docker compose down`.

Si se edita el codigo, hay que reconstruir la imagen antes de levantar de nuevo:
```
docker compose build
docker compose up -d
```

## URLs para usar

### maestra -> http://localhost:8083

| Metodo | URL | Descripcion |
|--------|-----|--------------|
| GET    | `http://localhost:8083/api/clientes`      | Listar clientes |
| GET    | `http://localhost:8083/api/clientes/{id}` | Obtener un cliente |
| POST   | `http://localhost:8083/api/clientes`      | Crear cliente |
| PUT    | `http://localhost:8083/api/clientes/{id}` | Actualizar cliente |
| DELETE | `http://localhost:8083/api/clientes/{id}` | Eliminar cliente |

Body (POST/PUT):
```json
{
  "dni": "12345678",
  "nombres": "Salvador",
  "apellidos": "Quispe",
  "celular": "999888777",
  "correo": "salvador@mail.com",
  "licencia": "Q12345678",
  "estado": "ACTIVO"
}
```

### transaccional -> http://localhost:8084

| Metodo | URL | Descripcion |
|--------|-----|--------------|
| GET    | `http://localhost:8084/api/alquileres`      | Listar alquileres |
| GET    | `http://localhost:8084/api/alquileres/{id}` | Obtener un alquiler |
| POST   | `http://localhost:8084/api/alquileres`      | Crear alquiler |
| PUT    | `http://localhost:8084/api/alquileres/{id}` | Actualizar alquiler |
| DELETE | `http://localhost:8084/api/alquileres/{id}` | Eliminar alquiler |

Body (POST/PUT), usa un `clienteId` (UUID) real que exista en `maestra`:
```json
{
  "clienteId": "<UUID_DE_UN_CLIENTE_REAL>",
  "vehiculoId": "veh-001",
  "dias": 5,
  "fechaInicio": "2026-07-05",
  "fechaFin": "2026-07-10",
  "total": 250.00,
  "estado": "PENDIENTE"
}
```
Estados: `PENDIENTE`, `EN_CURSO`, `FINALIZADO`, `CANCELADO`.

`transaccional` valida el `clienteId` llamando a `maestra` (`http://localhost:8083`). Si no existe, responde `400`.

## Como cambiar de puerto (por si 8083/8084 ya estan ocupados)

Los puertos son solo una convencion elegida para no chocar con `cliente-service`/`alquiler-service` (que usan 8081/8082). Si en algun momento necesitas otros puertos (ej: porque ya tienes algo corriendo en 8083/8084), hay que tocar **2 lugares**:

1. **`application.yml` de cada microservicio** (el puerto interno de la app):
   ```yaml
   server:
     port: 9083   # el puerto nuevo que quieras
   ```
   Si cambias el puerto de `maestra`, tambien hay que actualizar en `transaccional/src/main/resources/application.yml`:
   ```yaml
   maestra:
     service:
       url: ${MAESTRA_SERVICE_URL:http://localhost:9083}
   ```
   (el valor por defecto, solo se usa si no viene por variable de entorno).

2. **`docker-compose.yml`** (el mapeo puerto-maquina -> puerto-contenedor), si corres con Docker:
   ```yaml
   services:
     maestra:
       ports:
         - "9083:9083"   # host:contenedor, ambos deben coincidir con el server.port de arriba
   ```
   Y si cambias el puerto de `maestra`, actualizar tambien la variable de entorno que usa `transaccional` para encontrarla dentro de la red de Docker:
   ```yaml
     transaccional:
       environment:
         MAESTRA_SERVICE_URL: http://maestra:9083
   ```

No hace falta tocar nada mas: ni el codigo Java, ni el `Dockerfile` (el `EXPOSE` ahi es solo documentacion, no bloquea nada si el puerto real cambia por `application.yml`).

### Aplicar el cambio de puerto en Docker

Despues de editar `application.yml` y `docker-compose.yml`, hay que reconstruir la imagen (el puerto queda "horneado" dentro del jar):
```
docker compose down
docker compose build
docker compose up -d
```
Si ademas quieres que el cambio quede en Docker Hub (no solo en tu maquina):
```
docker push salvaqc/maestra:latest
docker push salvaqc/transaccional:latest
```

## Como funciona

- `maestra` guarda clientes, es autonomo.
- `transaccional` guarda alquileres; al crear uno, le pregunta a `maestra` por HTTP (`WebClient`) si el `clienteId` existe. Si no existe o `maestra` no responde, rechaza con `400`.
- Cada microservicio: `Controller` (recibe HTTP) -> `Service` (logica) -> `Repository` (habla con Postgres, reactivo) -> `Model` (entidad = fila de tabla).
- `cliente_id` en `alquileres` no es una foreign key real de Postgres (cada microservicio deberia poder tener su propia BD); se valida por HTTP, no por constraint.

## Schema SQL

Cada microservicio trae su `schema.sql` en `src/main/resources/` y Spring Boot lo ejecuta solo al arrancar (`CREATE TABLE IF NOT EXISTS`, no falla si ya existe).

### `maestra` (tabla `clientes`)
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS clientes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dni         VARCHAR(20)  NOT NULL,
    nombres     VARCHAR(100) NOT NULL,
    apellidos   VARCHAR(100) NOT NULL,
    celular     VARCHAR(20)  NOT NULL,
    correo      VARCHAR(150) NOT NULL,
    licencia    VARCHAR(50)  NOT NULL,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO'
                CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_clientes_dni UNIQUE (dni)
);

CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes (correo);
CREATE INDEX IF NOT EXISTS idx_clientes_estado ON clientes (estado);
```

### `transaccional` (tabla `alquileres`)
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS alquileres (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id    VARCHAR(50)   NOT NULL,
    vehiculo_id   VARCHAR(50)   NOT NULL,
    dias          INTEGER       NOT NULL CHECK (dias > 0),
    fecha_inicio  DATE          NOT NULL,
    fecha_fin     DATE          NOT NULL,
    total         NUMERIC(10,2) NOT NULL CHECK (total > 0),
    estado        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE'
                  CHECK (estado IN ('PENDIENTE', 'EN_CURSO', 'FINALIZADO', 'CANCELADO')),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE INDEX IF NOT EXISTS idx_alquileres_cliente_id  ON alquileres (cliente_id);
CREATE INDEX IF NOT EXISTS idx_alquileres_vehiculo_id ON alquileres (vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_alquileres_estado      ON alquileres (estado);
```

## Como agregar o cambiar tablas

El `schema.sql` es idempotente (`CREATE TABLE IF NOT EXISTS`) pero no migra tablas ya existentes. Para cambios sobre datos existentes, agregar `ALTER TABLE` debajo del `CREATE TABLE`.

- **Columna nueva**: `ALTER TABLE clientes ADD COLUMN IF NOT EXISTS direccion VARCHAR(200);` + agregar el campo con getter/setter en `Cliente.java`.
- **Estado nuevo**: `ALTER TABLE alquileres DROP CONSTRAINT IF EXISTS alquileres_estado_check;` seguido de `ADD CONSTRAINT ... CHECK (estado IN (...))` con el valor nuevo + actualizar el `@Pattern` en el modelo Java.
- **Tabla nueva**: `CREATE TABLE IF NOT EXISTS` en un `schema.sql`, mas su `Model` (`@Table`), `Repository` (`ReactiveCrudRepository<T, UUID>`), `Service` y `Controller`, siguiendo el mismo patron que `Cliente`/`Alquiler`. Si otro microservicio la necesita, se valida por HTTP (un `Client` nuevo), nunca con foreign key cruzada.

Reglas: tablas/columnas en snake_case y plural, `id UUID DEFAULT gen_random_uuid()`, `created_at TIMESTAMPTZ DEFAULT now()`, `CHECK` en vez de `ENUM`, indices en columnas que se filtran seguido.

## Estructura de carpetas

```
src/main/java/com/salvador/quispe/
  model/         -> entidad de la tabla
  repository/    -> ReactiveCrudRepository
  service/       -> logica de negocio
  rest/          -> endpoints REST (controller)
src/main/resources/
  application.yml -> puerto y conexion a Postgres
  schema.sql       -> crea la tabla automaticamente al arrancar
```
`transaccional` ademas tiene `client/` con el `WebClient` (`ClienteClient`) que llama a `maestra`.



DB_HOST=ep-twilight-union-adfs9lp2-pooler.c-2.us-east-1.aws.neon.tech
DB_PORT=5432
DB_NAME=neondb
DB_USER=neondb_owner
DB_PASSWORD=npg_2C6GiZwuFbWs

#colocar en los 3


ya estoy acá



estandar de java/hct/apellido/ nombre
misma estructura


todo lo demas igual, dockerisalo y suelo
