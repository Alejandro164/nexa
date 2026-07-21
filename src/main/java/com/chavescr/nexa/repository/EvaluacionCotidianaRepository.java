package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.EvaluacionCotidiana;

public interface EvaluacionCotidianaRepository extends JpaRepository<EvaluacionCotidiana, Long> {

    @Query("SELECT e FROM EvaluacionCotidiana e WHERE e.institucion.id = :institucionId " +
            "AND e.estudiante.nivelAcademico.id = :nivelId AND e.indicador.materia.id = :materiaId " +
            "ORDER BY e.fecha DESC")
    List<EvaluacionCotidiana> findByInstitucionIdAndNivelIdAndMateriaId(
            @Param("institucionId") Long institucionId, @Param("nivelId") Long nivelId,
            @Param("materiaId") Long materiaId);

    Optional<EvaluacionCotidiana> findByIdAndInstitucionId(Long id, Long institucionId);

    @Query("SELECT e FROM EvaluacionCotidiana e WHERE e.institucion.id = :institucionId " +
            "AND e.estudiante.nivelAcademico.id = :nivelId AND e.indicador.materia.id = :materiaId " +
            "AND e.indicador.periodo.id = :periodoId")
    List<EvaluacionCotidiana> findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoId(
            @Param("institucionId") Long institucionId, @Param("nivelId") Long nivelId,
            @Param("materiaId") Long materiaId, @Param("periodoId") Long periodoId);
}
