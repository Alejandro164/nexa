package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.IncidenteConducta;

public interface IncidenteConductaRepository extends JpaRepository<IncidenteConducta, Long> {

    List<IncidenteConducta> findByInstitucionIdAndPeriodoIdAndEstudianteId(
            Long institucionId, Long periodoId, Long estudianteId);

    List<IncidenteConducta> findByInstitucionIdAndPeriodoIdAndEstudianteIdIn(
            Long institucionId, Long periodoId, Collection<Long> estudianteIds);
}
