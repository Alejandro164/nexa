package com.chavescr.nexa.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.chavescr.nexa.service.TipoMateriaService;

/**
 * Los tipos de materia pasaron de catálogo global a uno por dirección. Este runner (idempotente) migra los
 * datos viejos: copia los tipos globales a cada dirección, reapunta materias y lecciones bloqueadas a la
 * copia de su dirección y borra los globales. En prod lo hace antes
 * docker/migraciones/2026-11-04-tipos-materia-direccion.sql; aquí los pasos quedan como no-op.
 * La columna {@code codigo} (marca de tipo del sistema) ya no se usa y se elimina.
 */
@Component
@Order(1)
public class TipoMateriaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TipoMateriaSeeder.class);

    /** Quita las únicas viejas de una sola columna (codigo, nombre); Hibernate les puso nombres aleatorios. */
    private static final String QUITAR_UNICAS_GLOBALES = """
            DO $$
            DECLARE r record;
            BEGIN
                FOR r IN SELECT con.conname FROM pg_constraint con
                         JOIN pg_class rel ON rel.oid = con.conrelid
                         WHERE rel.relname = 'tipos_materia' AND con.contype = 'u'
                           AND array_length(con.conkey, 1) = 1
                LOOP
                    EXECUTE format('ALTER TABLE tipos_materia DROP CONSTRAINT %I', r.conname);
                END LOOP;
            END $$""";

    private static final String COPIAR_A_DIRECCIONES = """
            INSERT INTO tipos_materia (direccion_id, nombre, orden, activo)
            SELECT d.id, g.nombre, g.orden, g.activo
            FROM direcciones d CROSS JOIN tipos_materia g
            WHERE g.direccion_id IS NULL
              AND NOT EXISTS (SELECT 1 FROM tipos_materia t
                              WHERE t.direccion_id = d.id
                                AND lower(t.nombre) = lower(g.nombre))""";

    private static final String REAPUNTAR = """
            UPDATE %s x SET tipo_materia_id = t.id
            FROM tipos_materia g, tipos_materia t
            WHERE x.tipo_materia_id = g.id AND g.direccion_id IS NULL
              AND t.direccion_id = x.direccion_id
              AND lower(t.nombre) = lower(g.nombre)""";

    private static final String BORRAR_GLOBALES = """
            DELETE FROM tipos_materia g
            WHERE g.direccion_id IS NULL
              AND NOT EXISTS (SELECT 1 FROM materias m WHERE m.tipo_materia_id = g.id)
              AND NOT EXISTS (SELECT 1 FROM seccion_bloqueo_lecciones b WHERE b.tipo_materia_id = g.id)""";

    /** Materias viejas que solo tenían el texto {@code tipo}: se enlazan por nombre dentro de su dirección. */
    private static final String ENLAZAR_POR_NOMBRE = """
            UPDATE materias m SET tipo_materia_id = t.id
            FROM tipos_materia t
            WHERE m.tipo_materia_id IS NULL AND m.tipo IS NOT NULL AND btrim(m.tipo) <> ''
              AND t.direccion_id = m.direccion_id AND lower(t.nombre) = lower(btrim(m.tipo))""";

    private final JdbcTemplate jdbcTemplate;
    private final TipoMateriaService tipoMateriaService;

    public TipoMateriaSeeder(JdbcTemplate jdbcTemplate, TipoMateriaService tipoMateriaService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tipoMateriaService = tipoMateriaService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ejecutar("quitar únicas globales", QUITAR_UNICAS_GLOBALES);
        ejecutar("quitar columna codigo", "ALTER TABLE tipos_materia DROP COLUMN IF EXISTS codigo");

        Integer globales = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tipos_materia WHERE direccion_id IS NULL", Integer.class);
        if (globales != null && globales > 0) {
            ejecutar("copiar tipos a cada dirección", COPIAR_A_DIRECCIONES);
            ejecutar("reapuntar materias", REAPUNTAR.formatted("materias"));
            ejecutar("reapuntar lecciones bloqueadas", REAPUNTAR.formatted("seccion_bloqueo_lecciones"));
            ejecutar("borrar tipos globales", BORRAR_GLOBALES);
            log.info("Tipos de materia migrados a cada dirección ({} globales)", globales);
        }

        List<Long> direcciones = jdbcTemplate.queryForList("SELECT id FROM direcciones", Long.class);
        for (Long direccionId : direcciones) {
            try {
                tipoMateriaService.listar(direccionId);
            } catch (Exception e) {
                log.warn("No se pudieron asegurar los tipos de materia de la dirección {}: {}",
                        direccionId, e.getMessage());
            }
        }
        int vinculadas = ejecutar("enlazar materias por nombre", ENLAZAR_POR_NOMBRE);
        log.info("Catálogo de tipos de materia listo. Materias vinculadas: {}", vinculadas);
    }

    private int ejecutar(String paso, String sql) {
        try {
            return jdbcTemplate.update(sql);
        } catch (Exception e) {
            log.warn("Tipos de materia — no se pudo {}: {}", paso, e.getMessage());
            return 0;
        }
    }
}
