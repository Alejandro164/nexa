package com.chavescr.nexa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIndexInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        log.info("=== Verificando índices de base de datos ===");

        String[] statements = {
                "CREATE INDEX IF NOT EXISTS idx_usuarios_email   ON usuarios (email)",
                "CREATE INDEX IF NOT EXISTS idx_usuarios_usuario ON usuarios (usuario)",
                "CREATE INDEX IF NOT EXISTS idx_usuarios_cedula  ON usuarios (cedula)",
                "CREATE INDEX IF NOT EXISTS idx_usuarios_nombre  ON usuarios (nombre)",
                "CREATE INDEX IF NOT EXISTS idx_instituciones_nombre ON instituciones (nombre)",
                "CREATE INDEX IF NOT EXISTS idx_instituciones_codigo ON instituciones (codigo)",
                "CREATE INDEX IF NOT EXISTS idx_user_roles_uid   ON usuario_roles (usuario_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_roles_rid   ON usuario_roles (rol_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_inst_uid    ON usuario_instituciones (usuario_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_inst_iid    ON usuario_instituciones (institucion_id)",
                "CREATE INDEX IF NOT EXISTS idx_bitacora_inst_fecha ON bitacora_evento (institucion_id, fecha DESC)",
                "CREATE INDEX IF NOT EXISTS idx_bitacora_inst_modulo ON bitacora_evento (institucion_id, modulo, fecha DESC)",
                "CREATE INDEX IF NOT EXISTS idx_incidente_conducta_inst_periodo ON incidentes_conducta (institucion_id, periodo_id)",
                "CREATE INDEX IF NOT EXISTS idx_incidente_conducta_inst_periodo_tipo ON incidentes_conducta (institucion_id, periodo_id, tipo)",
                "CREATE INDEX IF NOT EXISTS idx_incidente_conducta_estudiante_periodo ON incidentes_conducta (institucion_id, periodo_id, estudiante_id)"
        };

        for (String sql : statements) {
            try {
                jdbcTemplate.execute(sql);
                log.debug("Índice ejecutado: {}", sql.substring(0, Math.min(60, sql.length())));
            } catch (Exception e) {
                log.warn("No se pudo crear índice: {}", e.getMessage());
            }
        }

        log.info("=== Índices verificados ===");
        eliminarAmonestacionesConducta();
        eliminarExtraclase();
    }

    private void eliminarAmonestacionesConducta() {
        try {
            int eliminadas = jdbcTemplate.update("DELETE FROM incidentes_conducta WHERE tipo = 'AMONESTACION'");
            if (eliminadas > 0) {
                log.info("Se eliminaron {} amonestaciones de conducta estudiantil", eliminadas);
            }
        } catch (Exception e) {
            log.warn("No se pudieron eliminar amonestaciones de conducta: {}", e.getMessage());
        }
    }

    private void eliminarExtraclase() {
        try {
            Boolean existeColumna = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                            + "WHERE table_schema = 'public' AND table_name = 'distribuciones_porcentuales' "
                            + "AND column_name = 'trabajos_extraclase')",
                    Boolean.class);
            if (Boolean.TRUE.equals(existeColumna)) {
                jdbcTemplate.execute(
                        "UPDATE distribuciones_porcentuales SET cotidiano = cotidiano + COALESCE(trabajos_extraclase, 0)");
                jdbcTemplate.execute("ALTER TABLE distribuciones_porcentuales DROP COLUMN trabajos_extraclase");
                log.info("Columna trabajos_extraclase eliminada de distribuciones_porcentuales");
            }
            jdbcTemplate.execute("DROP TABLE IF EXISTS trabajos_calificaciones CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS trabajos_definicion CASCADE");
        } catch (Exception e) {
            log.warn("No se pudo limpiar datos de extraclase: {}", e.getMessage());
        }
    }
}
