package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.NivelAcademico;

public interface NivelAcademicoRepository extends JpaRepository<NivelAcademico, Long> {
    List<NivelAcademico> findByDireccionIdOrderByGradoAscSeccionAsc(Long direccionId);
    List<NivelAcademico> findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(Long direccionId);
    boolean existsByDireccionId(Long direccionId);

    boolean existsByDireccionIdAndActivoTrue(Long direccionId);
    Optional<NivelAcademico> findByIdAndDireccionId(Long id, Long direccionId);
}
