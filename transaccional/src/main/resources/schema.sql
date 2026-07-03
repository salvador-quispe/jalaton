-- Habilita gen_random_uuid() para generar IDs tipo UUID
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

-- clienteId/vehiculoId no son FK reales porque viven en otro microservicio/BD (maestra);
-- se validan por HTTP (ver ClienteClient), pero igual conviene indexarlos para consultas
CREATE INDEX IF NOT EXISTS idx_alquileres_cliente_id  ON alquileres (cliente_id);
CREATE INDEX IF NOT EXISTS idx_alquileres_vehiculo_id ON alquileres (vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_alquileres_estado      ON alquileres (estado);
