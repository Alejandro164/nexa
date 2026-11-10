-- ════════════════════════════════════════════════════════════════════════════
-- Ciclos educativos y una escala de notas por ciclo.
--
-- Cada sección (niveles_academicos) pasa a tener un ciclo: PRIMER_CICLO (1.º-3.º),
-- SEGUNDO_CICLO (4.º-6.º), TERCER_CICLO (7.º-9.º) o CUARTO_CICLO (10.º-12.º).
-- La escala de notas deja de ser una por dirección y pasa a ser una por dirección
-- y ciclo; los estudiantes usan la escala del ciclo de su sección.
--
-- Este script:
--   1. agrega niveles_academicos.ciclo y lo llena según el grado,
--   2. agrega escalas_notas.ciclo y quita la única vieja por dirección,
--   3. copia la escala de cada dirección a los cuatro ciclos y borra la original,
--   4. crea la única (direccion_id, ciclo).
-- Las notas se ven igual que antes hasta que el director edite la escala de un ciclo.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar la versión con ciclos.
-- En dev no hace falta: Hibernate crea las columnas y CicloEducativoSeeder hace
-- el resto al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez. Conserva todos los datos.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-10-ciclos-escala.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ── Ciclo de cada sección ───────────────────────────────────────────────────
ALTER TABLE niveles_academicos ADD COLUMN IF NOT EXISTS ciclo varchar(20);

UPDATE niveles_academicos SET ciclo = CASE
    WHEN grado BETWEEN 1 AND 3 THEN 'PRIMER_CICLO'
    WHEN grado BETWEEN 4 AND 6 THEN 'SEGUNDO_CICLO'
    WHEN grado BETWEEN 7 AND 9 THEN 'TERCER_CICLO'
    WHEN grado BETWEEN 10 AND 12 THEN 'CUARTO_CICLO'
END
WHERE ciclo IS NULL AND grado BETWEEN 1 AND 12;

ALTER TABLE niveles_academicos DROP CONSTRAINT IF EXISTS niveles_academicos_ciclo_check;
ALTER TABLE niveles_academicos ADD CONSTRAINT niveles_academicos_ciclo_check
    CHECK (ciclo IN ('PRIMER_CICLO', 'SEGUNDO_CICLO', 'TERCER_CICLO', 'CUARTO_CICLO'));

-- ── Escala por ciclo ────────────────────────────────────────────────────────
ALTER TABLE escalas_notas ADD COLUMN IF NOT EXISTS ciclo varchar(20);

DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT con.conname FROM pg_constraint con
             JOIN pg_class rel ON rel.oid = con.conrelid
             WHERE rel.relname = 'escalas_notas' AND con.contype = 'u'
               AND array_length(con.conkey, 1) = 1
    LOOP
        EXECUTE format('ALTER TABLE escalas_notas DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

INSERT INTO escalas_notas (direccion_id, ciclo, nota_minima, nota_maxima, decimales, nota_aprobacion, tramos)
SELECT e.direccion_id, c.ciclo, e.nota_minima, e.nota_maxima, e.decimales, e.nota_aprobacion, e.tramos
FROM escalas_notas e
CROSS JOIN (VALUES ('PRIMER_CICLO'), ('SEGUNDO_CICLO'), ('TERCER_CICLO'), ('CUARTO_CICLO')) AS c (ciclo)
WHERE e.ciclo IS NULL
  AND NOT EXISTS (SELECT 1 FROM escalas_notas x
                  WHERE x.direccion_id = e.direccion_id AND x.ciclo = c.ciclo);

DELETE FROM escalas_notas WHERE ciclo IS NULL;

ALTER TABLE escalas_notas DROP CONSTRAINT IF EXISTS escalas_notas_ciclo_check;
ALTER TABLE escalas_notas ADD CONSTRAINT escalas_notas_ciclo_check
    CHECK (ciclo IN ('PRIMER_CICLO', 'SEGUNDO_CICLO', 'TERCER_CICLO', 'CUARTO_CICLO'));

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_escala_notas_direccion_ciclo') THEN
        ALTER TABLE escalas_notas ADD CONSTRAINT uk_escala_notas_direccion_ciclo UNIQUE (direccion_id, ciclo);
    END IF;
END $$;

COMMIT;
