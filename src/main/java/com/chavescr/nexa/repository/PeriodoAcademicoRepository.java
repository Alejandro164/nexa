package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.PeriodoAcademico;

public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {
    List<PeriodoAcademico> findByDireccionIdOrderByFechaInicioDesc(Long direccionId);
    List<PeriodoAcademico> findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(Long direccionId);
    Optional<PeriodoAcademico> findByIdAndDireccionId(Long id, Long direccionId);
}
