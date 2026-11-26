-- ════════════════════════════════════════════════════════════════════════════
-- Nombres y emojis propios de los rubros en cada componente de evaluación.
--
-- Cada componente puede renombrar sus rubros (por ejemplo "Pruebas" → "Exámenes")
-- y elegirles un emoji. Esos nombres y emojis se ven en el formulario y la tabla
-- de componentes, en las pestañas de Gestión Académica (que ahora dependen de la
-- materia elegida) y en Notas y Promedio.
--   - nombre_*: NULL = nombre predeterminado del rubro.
--   - emoji_*:  NULL = emoji de la institución (tipos_componente.emoji).
--
-- Este script agrega las diez columnas como NULL, así todos los componentes
-- existentes siguen mostrándose igual hasta que alguien los personalice.
-- Requiere haber aplicado antes 2026-11-10-componentes-evaluacion.sql.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar esta versión. En dev Hibernate crea las columnas al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-25-nombres-emojis-rubros.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

ALTER TABLE componentes_evaluacion
    ADD COLUMN IF NOT EXISTS nombre_cotidiano  varchar(60),
    ADD COLUMN IF NOT EXISTS nombre_tareas     varchar(60),
    ADD COLUMN IF NOT EXISTS nombre_proyecto   varchar(60),
    ADD COLUMN IF NOT EXISTS nombre_pruebas    varchar(60),
    ADD COLUMN IF NOT EXISTS nombre_asistencia varchar(60),
    ADD COLUMN IF NOT EXISTS emoji_cotidiano   varchar(16),
    ADD COLUMN IF NOT EXISTS emoji_tareas      varchar(16),
    ADD COLUMN IF NOT EXISTS emoji_proyecto    varchar(16),
    ADD COLUMN IF NOT EXISTS emoji_pruebas     varchar(16),
    ADD COLUMN IF NOT EXISTS emoji_asistencia  varchar(16);

COMMIT;
