package com.chavescr.nexa.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;

public interface AsistenciaEstudianteRepository extends JpaRepository<AsistenciaEstudiante, Long> {

    List<AsistenciaEstudiante> findByDireccionIdAndNivelAcademicoIdAndFechaAndMateriaIdAndNumeroLeccion(
            Long direccionId, Long nivelId, LocalDate fecha, Long materiaId, Integer numeroLeccion);

    long countByDocumentoRuta(String documentoRuta);

    Optional<AsistenciaEstudiante> findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
            Long direccionId, Long estudianteId, LocalDate fecha, Long materiaId, Integer numeroLeccion);

    List<AsistenciaEstudiante> findByDireccionIdAndEstudianteIdAndMateriaIdAndFechaBetween(Long direccionId,
            Long estudianteId, Long materiaId, LocalDate desde, LocalDate hasta);

    /** Lecciones que no son presente, agrupadas por estudiante y estado, dentro de un período. */
    @Query("""
            SELECT a.estudiante.id, a.estado, COUNT(a)
            FROM AsistenciaEstudiante a
            WHERE a.direccion.id = :direccionId
              AND a.estudiante.id IN :estudianteIds
              AND a.fecha >= :desde AND a.fecha <= :hasta
              AND a.estado IS NOT NULL
              AND a.estado <> :presente
            GROUP BY a.estudiante.id, a.estado
            """)
    List<Object[]> contarEstadosPorEstudiante(@Param("direccionId") Long direccionId,
            @Param("estudianteIds") Collection<Long> estudianteIds,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("presente") EstadoAsistencia presente);

    @Query("""
            SELECT a.estudiante.id, a.materia.id, a.fecha, a.estado
            FROM AsistenciaEstudiante a
            WHERE a.direccion.id = :direccionId
              AND a.nivelAcademico.id IN :nivelIds
              AND a.fecha >= :desde AND a.fecha <= :hasta
            """)
    List<Object[]> findEstadosEntre(@Param("direccionId") Long direccionId,
            @Param("nivelIds") Collection<Long> nivelIds,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT a.materia.id, a.estado
            FROM AsistenciaEstudiante a
            WHERE a.direccion.id = :direccionId
              AND a.estudiante.id = :estudianteId
              AND a.fecha >= :desde AND a.fecha <= :hasta
            """)
    List<Object[]> findEstadosDeEstudiante(@Param("direccionId") Long direccionId,
            @Param("estudianteId") Long estudianteId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Lecciones que no cuentan como presente: ausencia o tardía, con la materia. */
    @Query("""
            SELECT a.fecha, a.numeroLeccion, a.estado, m.nombre, a.observaciones, p.nombre
            FROM AsistenciaEstudiante a
            JOIN a.materia m
            LEFT JOIN a.registradoPor p
            WHERE a.direccion.id = :direccionId
              AND a.estudiante.id = :estudianteId
              AND a.fecha >= :desde AND a.fecha <= :hasta
              AND a.estado IS NOT NULL
              AND a.estado <> :presente
            ORDER BY a.fecha, a.numeroLeccion, m.nombre
            """)
    List<Object[]> findAusenciasDeEstudiante(@Param("direccionId") Long direccionId,
            @Param("estudianteId") Long estudianteId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("presente") EstadoAsistencia presente);

    /** Cuántos estudiantes distintos tienen cada estado ese día (resumen de asistencia del inicio). */
    @Query("""
            SELECT a.estado, COUNT(DISTINCT a.estudiante.id)
            FROM AsistenciaEstudiante a
            WHERE a.direccion.id = :direccionId
              AND a.fecha = :fecha
              AND a.estado IS NOT NULL
            GROUP BY a.estado
            """)
    List<Object[]> contarEstudiantesPorEstado(@Param("direccionId") Long direccionId,
            @Param("fecha") LocalDate fecha);

    /** Lecciones (sección, materia, número) a las que ya se les pasó lista ese día. */
    @Query("""
            SELECT DISTINCT a.nivelAcademico.id, a.materia.id, a.numeroLeccion
            FROM AsistenciaEstudiante a
            WHERE a.direccion.id = :direccionId
              AND a.fecha = :fecha
              AND a.estado IS NOT NULL
            """)
    List<Object[]> findLeccionesConLista(@Param("direccionId") Long direccionId, @Param("fecha") LocalDate fecha);

    /** Estado de cada lección del día de estos estudiantes, sin las de presente. */
    @Query("""
            SELECT a.estudiante.id, a.estado
            FROM AsistenciaEstudiante a
            WHERE a.direccion.id = :direccionId
              AND a.fecha = :fecha
              AND a.estudiante.id IN :estudianteIds
              AND a.estado IS NOT NULL
              AND a.estado <> :presente
            """)
    List<Object[]> findNovedadesDelDia(@Param("direccionId") Long direccionId, @Param("fecha") LocalDate fecha,
            @Param("estudianteIds") Collection<Long> estudianteIds, @Param("presente") EstadoAsistencia presente);
}
