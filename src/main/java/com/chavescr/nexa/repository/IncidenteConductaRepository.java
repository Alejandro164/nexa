package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.IncidenteConducta;

public interface IncidenteConductaRepository extends JpaRepository<IncidenteConducta, Long> {

    List<IncidenteConducta> findByInstitucionIdAndPeriodoIdAndEstudianteId(
            Long institucionId, Long periodoId, Long estudianteId);

    @Query("SELECT i.estudiante.id, i FROM IncidenteConducta i "
            + "WHERE i.institucion.id = :institucionId AND i.periodo.id = :periodoId "
            + "AND i.estudiante.id IN :estudianteIds")
    List<Object[]> findDeEstudiantes(@Param("institucionId") Long institucionId,
            @Param("periodoId") Long periodoId, @Param("estudianteIds") Collection<Long> estudianteIds);
}
