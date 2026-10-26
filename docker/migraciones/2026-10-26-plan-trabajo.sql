-- ════════════════════════════════════════════════════════════════════════════
-- Agenda: "Proyectos" pasa a llamarse "Plan de trabajo"
--
--   proyectos         -> planes_trabajo
--   miembros_proyecto -> miembros_plan_trabajo
--   tareas_proyecto   -> tareas_plan_trabajo
--   proyecto_id       -> plan_trabajo_id   (en miembros_plan_trabajo y tareas_plan_trabajo)
--
-- No toca el "proyecto" del módulo de Notas (proyectos_definicion, proyectos_calificaciones).
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar la versión con Plan de trabajo. En dev también conviene
-- aplicarlo antes de levantar la app: con ddl-auto=update Hibernate crearía las
-- tablas nuevas vacías y dejaría los datos en las tablas viejas.
--
-- Es idempotente: se puede ejecutar más de una vez. Conserva todos los datos.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-10-26-plan-trabajo.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- Renombra una tabla, su secuencia de identidad y sus restricciones, solo si todavía tiene el nombre viejo.
CREATE OR REPLACE FUNCTION pg_temp.renombrar_tabla(viejo text, nuevo text, restricciones text[][]) RETURNS void AS $$
DECLARE
    par text[];
BEGIN
    IF to_regclass('public.' || viejo) IS NULL OR to_regclass('public.' || nuevo) IS NOT NULL THEN
        RETURN;
    END IF;
    EXECUTE format('ALTER TABLE public.%I RENAME TO %I', viejo, nuevo);
    IF to_regclass('public.' || viejo || '_id_seq') IS NOT NULL THEN
        EXECUTE format('ALTER SEQUENCE public.%I RENAME TO %I', viejo || '_id_seq', nuevo || '_id_seq');
    END IF;
    FOREACH par SLICE 1 IN ARRAY restricciones LOOP
        IF EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conrelid = ('public.' || nuevo)::regclass AND c.conname = par[1]) THEN
            EXECUTE format('ALTER TABLE public.%I RENAME CONSTRAINT %I TO %I', nuevo, par[1], par[2]);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.renombrar_tabla('proyectos', 'planes_trabajo', ARRAY[
    ['proyectos_pkey', 'planes_trabajo_pkey'],
    ['proyectos_estado_check', 'planes_trabajo_estado_check']]);

SELECT pg_temp.renombrar_tabla('miembros_proyecto', 'miembros_plan_trabajo', ARRAY[
    ['miembros_proyecto_pkey', 'miembros_plan_trabajo_pkey'],
    ['miembros_proyecto_rol_check', 'miembros_plan_trabajo_rol_check'],
    ['uk_miembro_proyecto_usuario', 'uk_miembro_plan_trabajo_usuario']]);

SELECT pg_temp.renombrar_tabla('tareas_proyecto', 'tareas_plan_trabajo', ARRAY[
    ['tareas_proyecto_pkey', 'tareas_plan_trabajo_pkey'],
    ['tareas_proyecto_estado_check', 'tareas_plan_trabajo_estado_check'],
    ['tareas_proyecto_prioridad_check', 'tareas_plan_trabajo_prioridad_check']]);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'miembros_plan_trabajo' AND column_name = 'proyecto_id') THEN
        ALTER TABLE miembros_plan_trabajo RENAME COLUMN proyecto_id TO plan_trabajo_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'tareas_plan_trabajo' AND column_name = 'proyecto_id') THEN
        ALTER TABLE tareas_plan_trabajo RENAME COLUMN proyecto_id TO plan_trabajo_id;
    END IF;
END $$;

COMMIT;
