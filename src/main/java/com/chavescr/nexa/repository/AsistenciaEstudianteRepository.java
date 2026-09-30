package com.chavescr.nexa.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.AsistenciaEstudiante;

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
}
