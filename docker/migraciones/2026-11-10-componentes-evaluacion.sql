-- ════════════════════════════════════════════════════════════════════════════
-- Componentes de evaluación (reemplazo de Distribución Porcentual).
--
-- Antes los porcentajes de cada rubro se guardaban por materia y período en
-- distribuciones_porcentuales. Ahora cada dirección tiene componentes de
-- evaluación (General y los casos de los artículos 40 y 41 del Reglamento de
-- Evaluación de los Aprendizajes) y cada materia tiene uno asignado.
--
-- Este script:
--   1. crea componentes_evaluacion y materias.componente_evaluacion_id,
--   2. siembra los 7 componentes en cada dirección que no tenga ninguno,
--   3. asigna "General" a las materias sin componente,
--   4. borra distribuciones_porcentuales (los porcentajes por período se descartan),
--   5. deja fijos los rubros: borra los tipos de componente propios (el cálculo
--      de notas nunca los usó) y activa los del sistema.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar la versión con componentes de evaluación.
-- En dev no hace falta: Hibernate crea la tabla y ComponenteEvaluacionSeeder hace
-- el resto al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-10-componentes-evaluacion.sql
-- Requiere haber aplicado antes 2026-11-04-tipos-materia-direccion.sql.
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ── Tabla de componentes ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS componentes_evaluacion (
    id           bigserial PRIMARY KEY,
    direccion_id bigint       NOT NULL REFERENCES direcciones (id),
    nombre       varchar(60)  NOT NULL,
    descripcion  varchar(160),
    cotidiano    integer      NOT NULL DEFAULT 0,
    tareas       integer      NOT NULL DEFAULT 0,
    proyecto     integer      NOT NULL DEFAULT 0,
    pruebas      integer      NOT NULL DEFAULT 0,
    asistencia   integer      NOT NULL DEFAULT 0,
    orden        integer      NOT NULL DEFAULT 0,
    activo       boolean      NOT NULL DEFAULT true,
    CONSTRAINT uk_componente_evaluacion_direccion_nombre UNIQUE (direccion_id, nombre)
);

-- ── Columna en materias ─────────────────────────────────────────────────────
ALTER TABLE materias ADD COLUMN IF NOT EXISTS componente_evaluacion_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_materia_componente_evaluacion') THEN
        ALTER TABLE materias ADD CONSTRAINT fk_materia_componente_evaluacion
            FOREIGN KEY (componente_evaluacion_id) REFERENCES componentes_evaluacion (id);
    END IF;
END $$;

-- ── Componentes predeterminados (solo en direcciones sin ninguno) ───────────
INSERT INTO componentes_evaluacion
    (direccion_id, nombre, descripcion, cotidiano, tareas, proyecto, pruebas, asistencia, orden, activo)
SELECT d.id, p.nombre, p.descripcion, p.cotidiano, p.tareas, p.proyecto, p.pruebas, p.asistencia, p.orden, true
FROM direcciones d
CROSS JOIN (VALUES
    ('General', 'Reparto por defecto de Nexa', 40, 15, 20, 20, 5, 1),
    ('I y II Ciclo — Básicas',
     'Art. 40 a) · Matemática, Estudios Sociales y Cívica, Ciencias, Español, Lengua Extranjera', 50, 10, 0, 35, 5, 2),
    ('I y II Ciclo — Complementarias',
     'Art. 40 b) · Ed. Musical, Ed. Física, Ed. para el Hogar, Artes Industriales', 60, 10, 0, 25, 5, 3),
    ('III Ciclo — Básicas',
     'Art. 41 a) · Matemática, Español, Estudios Sociales, Ciencias, Lengua Extranjera', 45, 10, 0, 40, 5, 4),
    ('III Ciclo — Complementarias',
     'Art. 41 b) · Cívica, Artes Plásticas, Ed. para el Hogar, Artes Industriales, Ed. Física, Ed. Musical, Psicología',
     45, 10, 40, 0, 5, 5),
    ('III Ciclo — Valor agregado', 'Art. 41 c)', 35, 10, 20, 30, 5, 6),
    ('Educación Religiosa', 'Art. 40 c) y 41 e)', 70, 25, 0, 0, 5, 7)
) AS p (nombre, descripcion, cotidiano, tareas, proyecto, pruebas, asistencia, orden)
WHERE NOT EXISTS (SELECT 1 FROM componentes_evaluacion c WHERE c.direccion_id = d.id);

-- ── Asignar "General" (o el primero, si no existe) a las materias sin componente
UPDATE materias m SET componente_evaluacion_id = (
    SELECT c.id FROM componentes_evaluacion c
    WHERE c.direccion_id = m.direccion_id
    ORDER BY (lower(c.nombre) = 'general') DESC, c.orden, c.nombre
    LIMIT 1)
WHERE m.componente_evaluacion_id IS NULL;

-- ── Distribución porcentual vieja ───────────────────────────────────────────
DROP TABLE IF EXISTS distribuciones_porcentuales;

-- ── Rubros fijos ────────────────────────────────────────────────────────────
DELETE FROM tipos_componente WHERE clave IS NULL;
UPDATE tipos_componente SET activo = true WHERE activo = false;

COMMIT;
