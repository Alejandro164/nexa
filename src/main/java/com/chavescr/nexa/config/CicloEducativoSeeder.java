package com.chavescr.nexa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Las secciones pasaron a tener ciclo y la escala de notas pasó de una por dirección a una por ciclo. Este runner
 * (idempotente) asigna a cada sección el ciclo de su grado y copia la escala que tenía cada dirección a los cuatro
 * ciclos, así las notas se ven igual que antes hasta que el director edite un ciclo. Corre después de
 * DataInitializer para cubrir también las secciones sembradas en dev. En prod lo hace antes
 * docker/migraciones/2026-11-10-ciclos-escala.sql.
 */
@Component
@Order(31)
public class CicloEducativoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CicloEducativoSeeder.class);

    static final String CICLO_POR_GRADO = """
            UPDATE niveles_academicos SET ciclo = CASE
                WHEN grado BETWEEN 1 AND 3 THEN 'PRIMER_CICLO'
                WHEN grado BETWEEN 4 AND 6 THEN 'SEGUNDO_CICLO'
                WHEN grado BETWEEN 7 AND 9 THEN 'TERCER_CICLO'
                WHEN grado BETWEEN 10 AND 12 THEN 'CUARTO_CICLO'
            END
            WHERE ciclo IS NULL AND grado BETWEEN 1 AND 12""";

    /** Quita la única vieja de una sola columna (direccion_id); Hibernate le puso un nombre aleatorio. */
    static final String QUITAR_UNICA_POR_DIRECCION = """
            DO $$
            DECLARE r record;
            BEGIN
                FOR r IN SELECT con.conname FROM pg_constraint con
                         JOIN pg_class rel ON rel.oid = con.conrelid
                         WHERE rel.relname = 'escalas_notas' AND con.contype = 'u'
                           AND array_length(con.conkey, 1) = 1
                LOOP
                    EXECUTE format('ALTER TABLE escalas_notas DROP CONSTRAINT %I', r.conname);
                END LOOP;
            END $$""";

    static final String COPIAR_A_CICLOS = """
            INSERT INTO escalas_notas (direccion_id, ciclo, nota_minima, nota_maxima, decimales, nota_aprobacion, tramos)
            SELECT e.direccion_id, c.ciclo, e.nota_minima, e.nota_maxima, e.decimales, e.nota_aprobacion, e.tramos
            FROM escalas_notas e
            CROSS JOIN (VALUES ('PRIMER_CICLO'), ('SEGUNDO_CICLO'), ('TERCER_CICLO'), ('CUARTO_CICLO')) AS c (ciclo)
            WHERE e.ciclo IS NULL
              AND NOT EXISTS (SELECT 1 FROM escalas_notas x
                              WHERE x.direccion_id = e.direccion_id AND x.ciclo = c.ciclo)""";

    static final String BORRAR_SIN_CICLO = "DELETE FROM escalas_notas WHERE ciclo IS NULL";

    private final JdbcTemplate jdbcTemplate;

    public CicloEducativoSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int secciones = ejecutar("asignar ciclo a las secciones", CICLO_POR_GRADO);
        ejecutar("quitar única por dirección", QUITAR_UNICA_POR_DIRECCION);
        int copiadas = ejecutar("copiar la escala a cada ciclo", COPIAR_A_CICLOS);
        ejecutar("borrar escalas sin ciclo", BORRAR_SIN_CICLO);
        log.info("Ciclos listos. Secciones con ciclo asignado: {}. Escalas copiadas a ciclos: {}", secciones, copiadas);
    }

    private int ejecutar(String paso, String sql) {
        try {
            return jdbcTemplate.update(sql);
        } catch (Exception e) {
            log.warn("Ciclos — no se pudo {}: {}", paso, e.getMessage());
            return 0;
        }
    }
}
