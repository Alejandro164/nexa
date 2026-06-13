package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.PeriodoAcademico;

public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {
    List<PeriodoAcademico> findByInstitucionIdOrderByFechaInicioDesc(Long institucionId);
    List<PeriodoAcademico> findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(Long institucionId);
    Optional<PeriodoAcademico> findByIdAndInstitucionId(Long id, Long institucionId);
}
