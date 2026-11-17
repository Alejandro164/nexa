-- ════════════════════════════════════════════════════════════════════════════
-- Modo de cálculo de la asistencia: reglamento del MEP o valores propios.
--
-- Algunos colegios no siguen el artículo 37 del Reglamento de Evaluación. Ahora
-- cada dirección elige:
--   - REGLAMENTO_MEP: tabla del artículo 37 (por porcentaje de ausencias), que va
--     al rubro Asistencia de la nota. Es el valor por defecto y lo que ya regía.
--   - PERSONALIZADO: "cada N registros bajan P puntos", y destino_rebaja_asistencia
--     decide si rebajan el rubro Asistencia (COMPONENTE) o la conducta (CONDUCTA).
--
-- Este script agrega rebajas_conducta.modo_asistencia con REGLAMENTO_MEP por
-- defecto, así nada cambia hasta que una dirección elija valores propios.
-- Requiere haber aplicado antes 2026-11-10-asistencia-reglamento.sql.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar esta versión. En dev Hibernate crea la columna al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-17-modo-asistencia.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

ALTER TABLE rebajas_conducta
    ADD COLUMN IF NOT EXISTS modo_asistencia varchar(20) NOT NULL DEFAULT 'REGLAMENTO_MEP';

ALTER TABLE rebajas_conducta DROP CONSTRAINT IF EXISTS rebajas_conducta_modo_asistencia_check;
ALTER TABLE rebajas_conducta ADD CONSTRAINT rebajas_conducta_modo_asistencia_check
    CHECK (modo_asistencia IN ('REGLAMENTO_MEP', 'PERSONALIZADO'));

COMMIT;
