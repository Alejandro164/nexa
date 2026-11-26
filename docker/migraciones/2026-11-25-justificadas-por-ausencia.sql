-- ════════════════════════════════════════════════════════════════════════════
-- Ausencias justificadas que forman una injustificada.
--
-- Cada dirección puede indicar cuántas ausencias justificadas forman una
-- ausencia injustificada (Escala de notas → Asistencia):
--   - 0: las justificadas no cuentan. Es el valor por defecto y lo que ya regía.
--   - N (1 a 30): cada grupo completo de N justificadas suma una injustificada;
--     las que no completan el grupo no cuentan.
-- Con el reglamento del MEP suman al porcentaje de ausencias del artículo 37.
-- Con valores propios entran a la regla de "Ausencia injustificada" y la regla
-- propia de "Ausencia justificada" deja de aplicar mientras N sea mayor que 0.
--
-- Este script agrega rebajas_conducta.justificadas_por_ausencia con 0 por
-- defecto, así nada cambia hasta que una dirección lo configure.
-- Requiere haber aplicado antes 2026-11-17-modo-asistencia.sql.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar esta versión. En dev Hibernate crea la columna al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-25-justificadas-por-ausencia.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

ALTER TABLE rebajas_conducta
    ADD COLUMN IF NOT EXISTS justificadas_por_ausencia integer NOT NULL DEFAULT 0;

ALTER TABLE rebajas_conducta DROP CONSTRAINT IF EXISTS rebajas_conducta_justificadas_por_ausencia_check;
ALTER TABLE rebajas_conducta ADD CONSTRAINT rebajas_conducta_justificadas_por_ausencia_check
    CHECK (justificadas_por_ausencia BETWEEN 0 AND 30);

COMMIT;
