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
                "CREATE INDEX IF NOT EXISTS idx_direcciones_nombre ON direcciones (nombre)",
                "CREATE INDEX IF NOT EXISTS idx_direcciones_codigo ON direcciones (codigo)",
                "CREATE INDEX IF NOT EXISTS idx_user_roles_uid   ON usuario_roles (usuario_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_roles_rid   ON usuario_roles (rol_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_inst_uid    ON usuario_direcciones (usuario_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_inst_iid    ON usuario_direcciones (direccion_id)",
                "CREATE INDEX IF NOT EXISTS idx_bitacora_inst_fecha ON bitacora_evento (direccion_id, fecha DESC)",
                "CREATE INDEX IF NOT EXISTS idx_bitacora_inst_modulo ON bitacora_evento (direccion_id, modulo, fecha DESC)",
                "CREATE INDEX IF NOT EXISTS idx_incidente_conducta_inst_periodo ON incidentes_conducta (direccion_id, periodo_id)",
                "CREATE INDEX IF NOT EXISTS idx_incidente_conducta_inst_periodo_tipo ON incidentes_conducta (direccion_id, periodo_id, tipo)",
                "CREATE INDEX IF NOT EXISTS idx_incidente_conducta_estudiante_periodo ON incidentes_conducta (direccion_id, periodo_id, estudiante_id)"
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
        copiarRubrosAComponentes();
    }

    private void copiarRubrosAComponentes() {
        copiar("indicadores_cotidiano", "COTIDIANO", "fecha", false);
        copiar("tareas_definicion", "TAREA", "fecha_entrega", false);
        copiar("proyectos_definicion", "PROYECTO", "fecha", true);
        copiar("examenes", "EXAMEN", "fecha", true);
        migrarResultados("evaluaciones_cotidiano", "indicador_id", "COTIDIANO", "cal.periodo_id");
        migrarResultados("tareas_calificaciones", "tarea_definicion_id", "TAREA", "cal.periodo_id");
        migrarResultados("proyectos_calificaciones", "proyecto_definicion_id", "PROYECTO", "c.periodo_id");
        migrarResultados("notas_examen", "examen_id", "EXAMEN", "c.periodo_id");
    }

    private void migrarResultados(String tabla, String columnaOrigen, String clave, String periodo) {
        if (!existeTabla(tabla)) {
            return;
        }
        enlazar(tabla, columnaOrigen, clave);
        if (copiarResultados(tabla, periodo)) {
            eliminarTabla(tabla);
        }
    }

    private boolean copiarResultados(String tabla, String periodo) {
        try {
            jdbcTemplate.execute(
                    "INSERT INTO resultados_componente (componente_id, estudiante_id, periodo_id, calificacion, "
                            + "puntos_obtenidos, observacion) "
                            + "SELECT cal.componente_id, cal.estudiante_id, " + periodo + ", cal.calificacion, "
                            + "cal.puntos_obtenidos, cal.observacion FROM " + tabla + " cal "
                            + "JOIN componentes c ON c.id = cal.componente_id "
                            + "WHERE cal.componente_id IS NOT NULL AND " + periodo + " IS NOT NULL "
                            + "AND NOT EXISTS (SELECT 1 FROM resultados_componente r "
                            + "WHERE r.componente_id = cal.componente_id AND r.estudiante_id = cal.estudiante_id "
                            + "AND r.periodo_id = " + periodo + ")");
            return true;
        } catch (Exception e) {
            log.warn("No se copiaron resultados de {}: {}", tabla, e.getMessage());
            return false;
        }
    }

    private boolean existeTabla(String tabla) {
        Boolean existe = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, tabla);
        return Boolean.TRUE.equals(existe);
    }

    private void eliminarTabla(String tabla) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + tabla + " CASCADE");
        log.info("Tabla {} eliminada", tabla);
    }

    private void copiar(String tabla, String clave, String columnaFecha, boolean conPeriodo) {
        try {
            String periodo = conPeriodo ? "periodo_id" : "NULL";
            jdbcTemplate.execute(
                    "INSERT INTO componentes (direccion_id, clave, periodo_id, nivel_id, materia_id, titulo, "
                            + "descripcion, fecha, porcentaje, puntos_totales, origen_id) "
                            + "SELECT direccion_id, '" + clave + "', " + periodo + ", nivel_id, materia_id, titulo, "
                            + "descripcion, " + columnaFecha + ", porcentaje, puntos_totales, id FROM " + tabla + " origen "
                            + "WHERE NOT EXISTS (SELECT 1 FROM componentes c WHERE c.clave = '" + clave
                            + "' AND c.origen_id = origen.id)");
        } catch (Exception e) {
            log.warn("No se copiaron rubros de {}: {}", tabla, e.getMessage());
        }
    }

    private void enlazar(String tabla, String columnaOrigen, String clave) {
        try {
            jdbcTemplate.update(
                    "UPDATE " + tabla + " cal SET componente_id = c.id FROM componentes c "
                            + "WHERE c.clave = ? AND c.origen_id = cal." + columnaOrigen
                            + " AND cal.componente_id IS NULL",
                    clave);
        } catch (Exception e) {
            log.warn("No se enlazaron calificaciones de {}: {}", tabla, e.getMessage());
        }
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
