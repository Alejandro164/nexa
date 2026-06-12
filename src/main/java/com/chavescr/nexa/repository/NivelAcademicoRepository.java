package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.NivelAcademico;

public interface NivelAcademicoRepository extends JpaRepository<NivelAcademico, Long> {
    List<NivelAcademico> findByInstitucionIdOrderByGradoAscSeccionAsc(Long institucionId);
    List<NivelAcademico> findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(Long institucionId);
    Optional<NivelAcademico> findByIdAndInstitucionId(Long id, Long institucionId);
}
