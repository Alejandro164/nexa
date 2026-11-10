-- ════════════════════════════════════════════════════════════════════════════
-- Asistencia según el artículo 37 del Reglamento de Evaluación de los Aprendizajes.
--
-- El rubro Asistencia de cada materia deja de restar puntos por cada ausencia o
-- tardía y pasa a asignarse según el porcentaje de ausencias injustificadas sobre
-- las lecciones impartidas (0-10% = 5 de 5, 10-20% = 4, …, 50% o más = 0).
-- Las ausencias y tardías ya no rebajan la conducta: van siempre al componente.
--
-- Este script:
--   1. agrega rebajas_conducta.tardias_por_ausencia (2 = cada tardía injustificada
--      es media ausencia, 1 = una ausencia), con 2 por defecto,
--   2. deja destino_rebaja_asistencia en COMPONENTE en todas las direcciones.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar la versión con la asistencia del reglamento.
-- En dev no hace falta: Hibernate crea la columna y DatabaseIndexInitializer
-- ajusta el destino al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-10-asistencia-reglamento.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

ALTER TABLE rebajas_conducta ADD COLUMN IF NOT EXISTS tardias_por_ausencia integer NOT NULL DEFAULT 2;

ALTER TABLE rebajas_conducta ALTER COLUMN destino_rebaja_asistencia SET DEFAULT 'COMPONENTE';
UPDATE rebajas_conducta SET destino_rebaja_asistencia = 'COMPONENTE'
WHERE destino_rebaja_asistencia <> 'COMPONENTE';

COMMIT;
