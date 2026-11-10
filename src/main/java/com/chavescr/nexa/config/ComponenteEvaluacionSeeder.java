package com.chavescr.nexa.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.chavescr.nexa.service.ComponenteEvaluacionService;

/**
 * Los componentes de evaluación reemplazan a la distribución porcentual por materia y período. Este runner
 * (idempotente) asegura los componentes de cada dirección, asigna "General" a las materias sin componente,
 * borra la tabla vieja y deja fijos los rubros: los del sistema activos y sin tipos propios, que el cálculo de
 * notas nunca usó. Corre después de DataInitializer para cubrir también las materias sembradas en dev.
 * En prod lo hace antes docker/migraciones/2026-11-10-componentes-evaluacion.sql.
 */
@Component
@Order(30)
public class ComponenteEvaluacionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ComponenteEvaluacionSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final ComponenteEvaluacionService componenteEvaluacionService;

    public ComponenteEvaluacionSeeder(JdbcTemplate jdbcTemplate,
            ComponenteEvaluacionService componenteEvaluacionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.componenteEvaluacionService = componenteEvaluacionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int asignadas = 0;
        List<Long> direcciones = jdbcTemplate.queryForList("SELECT id FROM direcciones", Long.class);
        for (Long direccionId : direcciones) {
            try {
                asignadas += componenteEvaluacionService.asignarAMateriasSinComponente(direccionId);
            } catch (Exception e) {
                log.warn("No se pudieron asegurar los componentes de evaluación de la dirección {}: {}",
                        direccionId, e.getMessage());
            }
        }
        ejecutar("borrar distribuciones porcentuales", "DROP TABLE IF EXISTS distribuciones_porcentuales");
        ejecutar("quitar tipos de componente propios", "DELETE FROM tipos_componente WHERE clave IS NULL");
        ejecutar("activar rubros del sistema", "UPDATE tipos_componente SET activo = true WHERE activo = false");
        log.info("Componentes de evaluación listos. Materias asignadas: {}", asignadas);
    }

    private void ejecutar(String paso, String sql) {
        try {
            jdbcTemplate.update(sql);
        } catch (Exception e) {
            log.warn("Componentes de evaluación — no se pudo {}: {}", paso, e.getMessage());
        }
    }
}
