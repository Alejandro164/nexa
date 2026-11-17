-- ════════════════════════════════════════════════════════════════════════════
-- Chat Asistente (Grok de xAI): configuración del sistema.
--
-- Crea la tabla asistente_configuracion, con una sola fila para todo el sistema que
-- administra System Config en Configuración → Integraciones → Asistente IA:
--   - api_key_cifrada: la API key de xAI, cifrada con la misma clave que WhatsApp
--     (whatsapp.encryption.key).
--   - modelo: grok-4.7 por defecto.
--   - activo: si el botón del chat se muestra a los usuarios. Empieza desactivado.
-- Las conversaciones no se guardan en la base: viven solo en la sesión.
--
-- Prod usa spring.jpa.hibernate.ddl-auto=validate, así que esto debe aplicarse
-- ANTES de desplegar esta versión. En dev Hibernate crea la tabla al arrancar.
--
-- Es idempotente: se puede ejecutar más de una vez.
-- Uso (en el servidor de prod):
--   psql -h localhost -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 2026-11-17-asistente-configuracion.sql
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

CREATE TABLE IF NOT EXISTS asistente_configuracion (
    id                    bigserial PRIMARY KEY,
    api_key_cifrada       text,
    modelo                varchar(60) NOT NULL DEFAULT 'grok-4.7',
    activo                boolean     NOT NULL DEFAULT false,
    fecha_actualizacion   timestamp(6),
    ultima_prueba_exitosa timestamp(6)
);

COMMIT;
