-- ════════════════════════════════════════════════════════════════════════════
-- Tipos de materia por dirección.
--
-- tipos_materia era un catálogo global (codigo y nombre únicos en toda la tabla).
-- Ahora cada dirección tiene sus propios tipos, que el director administra.
-- La columna codigo (marca de "tipo del sistema") ya no se usa y se elimina.
--
-- Este script:
--   1. agrega tipos_materia.direccion_id (FK a direcciones),
--   2. quita las restricciones únicas globales y la columna codigo,
--   3. copia cada tipo global a cada dirección,
--   4. reapunta materias y seccion_bloqueo_lecciones a la copia de su dirección,
--   5. borra los tipos globales,
--   6. crea la única por dirección (direccion_id, nombre).
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar la versión con tipos de materia por dirección.
-- En dev no hace falta: TipoMateriaSeeder hace la misma migración al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez. Conserva todos los datos.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-04-tipos-materia-direccion.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ── Columna de dirección ────────────────────────────────────────────────────
ALTER TABLE tipos_materia ADD COLUMN IF NOT EXISTS direccion_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tipo_materia_direccion') THEN
        ALTER TABLE tipos_materia ADD CONSTRAINT fk_tipo_materia_direccion
            FOREIGN KEY (direccion_id) REFERENCES direcciones (id);
    END IF;
END $$;

-- ── Quitar únicas globales (codigo, nombre) y la columna codigo ─────────────
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT con.conname FROM pg_constraint con
             JOIN pg_class rel ON rel.oid = con.conrelid
             WHERE rel.relname = 'tipos_materia' AND con.contype = 'u'
               AND array_length(con.conkey, 1) = 1
    LOOP
        EXECUTE format('ALTER TABLE tipos_materia DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE tipos_materia DROP COLUMN IF EXISTS codigo;

-- ── Copiar los tipos globales a cada dirección ──────────────────────────────
INSERT INTO tipos_materia (direccion_id, nombre, orden, activo)
SELECT d.id, g.nombre, g.orden, g.activo
FROM direcciones d CROSS JOIN tipos_materia g
WHERE g.direccion_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM tipos_materia t
                  WHERE t.direccion_id = d.id
                    AND lower(t.nombre) = lower(g.nombre));

-- ── Reapuntar materias y lecciones bloqueadas ───────────────────────────────
UPDATE materias x SET tipo_materia_id = t.id
FROM tipos_materia g, tipos_materia t
WHERE x.tipo_materia_id = g.id AND g.direccion_id IS NULL
  AND t.direccion_id = x.direccion_id
  AND lower(t.nombre) = lower(g.nombre);

UPDATE seccion_bloqueo_lecciones x SET tipo_materia_id = t.id
FROM tipos_materia g, tipos_materia t
WHERE x.tipo_materia_id = g.id AND g.direccion_id IS NULL
  AND t.direccion_id = x.direccion_id
  AND lower(t.nombre) = lower(g.nombre);

-- ── Borrar los globales que ya no usa nadie ─────────────────────────────────
DELETE FROM tipos_materia g
WHERE g.direccion_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM materias m WHERE m.tipo_materia_id = g.id)
  AND NOT EXISTS (SELECT 1 FROM seccion_bloqueo_lecciones b WHERE b.tipo_materia_id = g.id);

-- ── Única por dirección ─────────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tipo_materia_direccion_nombre') THEN
        ALTER TABLE tipos_materia ADD CONSTRAINT uk_tipo_materia_direccion_nombre
            UNIQUE (direccion_id, nombre);
    END IF;
END $$;

COMMIT;
