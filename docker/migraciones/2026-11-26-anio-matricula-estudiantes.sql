-- ════════════════════════════════════════════════════════════════════════════
-- Período (año de matrícula) en usuarios estudiantes.
--
-- Agrega la columna usuarios.anio_matricula para registrar el año lectivo en
-- que fue matriculado cada estudiante y permitir filtrar expedientes por
-- período desde el módulo de Estudiantes.
--
-- La columna queda nullable para cuentas que no son estudiantes (docentes,
-- padres, administradores). Para los estudiantes ya existentes sin valor, se
-- inicializa con el año actual.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar esta versión. En dev Hibernate crea la columna al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-26-anio-matricula-estudiantes.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS anio_matricula integer;

ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_anio_matricula_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_anio_matricula_check
    CHECK (anio_matricula IS NULL OR anio_matricula BETWEEN 1990 AND 2100);

UPDATE usuarios u
SET anio_matricula = EXTRACT(YEAR FROM CURRENT_DATE)::integer
WHERE u.anio_matricula IS NULL
  AND EXISTS (
      SELECT 1
      FROM usuario_roles ur
      JOIN roles r ON r.id = ur.rol_id
      WHERE ur.usuario_id = u.id
        AND r.nombre = 'ROLE_ESTUDIANTE'
  );

COMMIT;
