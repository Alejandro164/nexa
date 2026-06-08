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
                "CREATE INDEX IF NOT EXISTS idx_empresas_nombre  ON empresas (nombre)",
                "CREATE INDEX IF NOT EXISTS idx_empresas_cedula  ON empresas (cedula)",
                "CREATE INDEX IF NOT EXISTS idx_user_roles_uid   ON usuario_roles (usuario_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_roles_rid   ON usuario_roles (rol_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_emp_uid     ON usuario_empresas (usuario_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_emp_eid     ON usuario_empresas (empresa_id)"
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
    }
}
