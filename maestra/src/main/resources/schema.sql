-- Habilita gen_random_uuid() para generar IDs tipo UUID
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

-- Busquedas frecuentes por correo (ademas del DNI, que ya tiene indice unico)
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes (correo);
CREATE INDEX IF NOT EXISTS idx_clientes_estado ON clientes (estado);
