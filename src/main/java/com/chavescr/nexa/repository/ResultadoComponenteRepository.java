package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.ResultadoComponente;

public interface ResultadoComponenteRepository extends JpaRepository<ResultadoComponente, Long> {

    List<ResultadoComponente> findByComponenteIdIn(List<Long> componenteIds);

    List<ResultadoComponente> findByComponenteIdInAndPeriodoId(List<Long> componenteIds, Long periodoId);

    List<ResultadoComponente> findByComponenteIdAndPeriodoId(Long componenteId, Long periodoId);

    Optional<ResultadoComponente> findByComponenteIdAndEstudianteIdAndPeriodoId(Long componenteId, Long estudianteId,
            Long periodoId);

    boolean existsByComponenteId(Long componenteId);

    List<ResultadoComponente> findByComponente_Direccion_IdAndComponente_Nivel_IdAndComponente_Materia_IdAndPeriodo_Id(
            Long direccionId, Long nivelId, Long materiaId, Long periodoId);

    @Query("""
            SELECT r FROM ResultadoComponente r
            JOIN FETCH r.componente c
            JOIN FETCH c.materia
            JOIN FETCH c.nivel
            JOIN FETCH r.estudiante
            JOIN FETCH r.periodo
            WHERE c.direccion.id = :direccionId
              AND r.periodo.id IN :periodoIds
              AND c.nivel.id IN :nivelIds
            """)
    List<ResultadoComponente> findNotasDePeriodos(@Param("direccionId") Long direccionId,
            @Param("periodoIds") Collection<Long> periodoIds,
            @Param("nivelIds") Collection<Long> nivelIds);

    /** Notas de un estudiante en un período. Solo el componente y su materia, para no traer el grafo completo. */
    @Query("""
            SELECT r FROM ResultadoComponente r
            JOIN FETCH r.componente c
            JOIN FETCH c.materia
            WHERE c.direccion.id = :direccionId
              AND c.nivel.id = :nivelId
              AND r.estudiante.id = :estudianteId
              AND r.periodo.id = :periodoId
            """)
    List<ResultadoComponente> findDeEstudianteEnPeriodo(@Param("direccionId") Long direccionId,
            @Param("nivelId") Long nivelId, @Param("estudianteId") Long estudianteId,
            @Param("periodoId") Long periodoId);

    /** Materias de la sección que ya tienen alguna nota en esos períodos, sin traer las calificaciones. */
    @Query("""
            SELECT DISTINCT c.materia.id, c.materia.nombre
            FROM ResultadoComponente r
            JOIN r.componente c
            WHERE c.direccion.id = :direccionId
              AND c.nivel.id = :nivelId
              AND r.periodo.id IN :periodoIds
            """)
    List<Object[]> findMateriasConNota(@Param("direccionId") Long direccionId,
            @Param("nivelId") Long nivelId, @Param("periodoIds") Collection<Long> periodoIds);
}
