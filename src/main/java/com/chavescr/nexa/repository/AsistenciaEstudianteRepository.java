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
}
