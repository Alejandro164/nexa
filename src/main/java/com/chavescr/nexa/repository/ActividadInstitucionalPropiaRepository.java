package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ActividadInstitucionalPropia;

public interface ActividadInstitucionalPropiaRepository extends JpaRepository<ActividadInstitucionalPropia, Long> {

    List<ActividadInstitucionalPropia> findByInstitucionIdOrderByFechaInicioAsc(Long institucionId);

    Optional<ActividadInstitucionalPropia> findByIdAndInstitucionId(Long id, Long institucionId);
}
