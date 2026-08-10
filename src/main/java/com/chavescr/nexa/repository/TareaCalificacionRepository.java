package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.TareaCalificacion;

public interface TareaCalificacionRepository extends JpaRepository<TareaCalificacion, Long> {
    List<TareaCalificacion> findByInstitucionIdAndTareaDefinicionIdAndPeriodoId(
            Long institucionId, Long tareaDefinicionId, Long periodoId);

    List<TareaCalificacion> findByInstitucionIdAndTareaDefinicionIdInAndPeriodoId(
            Long institucionId, List<Long> tareaDefinicionIds, Long periodoId);

    Optional<TareaCalificacion> findByInstitucionIdAndEstudianteIdAndTareaDefinicionIdAndPeriodoId(
            Long institucionId, Long estudianteId, Long tareaDefinicionId, Long periodoId);

    boolean existsByTareaDefinicionId(Long tareaDefinicionId);

    @Query("SELECT t FROM TareaCalificacion t WHERE t.institucion.id = :institucionId " +
            "AND t.estudiante.nivelAcademico.id = :nivelId AND t.tareaDefinicion.materia.id = :materiaId " +
            "AND t.periodo.id = :periodoId")
    List<TareaCalificacion> findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoId(
            @Param("institucionId") Long institucionId, @Param("nivelId") Long nivelId,
            @Param("materiaId") Long materiaId, @Param("periodoId") Long periodoId);
}
