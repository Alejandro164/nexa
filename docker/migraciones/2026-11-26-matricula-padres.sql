-- ════════════════════════════════════════════════════════════════════════════
-- Vínculo entre prematrículas/borradores de matrícula y padres/encargados.
--
-- Permite asociar padres o encargados desde las etapas de Prematrícula y
-- Matrícula (borrador), y transferir automáticamente esos vínculos a
-- padres_estudiantes cuando la matrícula se completa y pasa a Expedientes.
--
-- Requiere haber aplicado antes 2026-11-26-matriculas-estudiantes.sql.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar esta versión. En dev Hibernate crea la tabla al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-26-matricula-padres.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

CREATE TABLE IF NOT EXISTS matricula_padres (
    matricula_id bigint NOT NULL REFERENCES matriculas_estudiantes(id) ON DELETE CASCADE,
    padre_id bigint NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    PRIMARY KEY (matricula_id, padre_id)
);

COMMIT;
