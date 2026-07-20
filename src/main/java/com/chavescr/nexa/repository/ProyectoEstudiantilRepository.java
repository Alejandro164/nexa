package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ProyectoEstudiantil;

public interface ProyectoEstudiantilRepository extends JpaRepository<ProyectoEstudiantil, Long> {
    List<ProyectoEstudiantil> findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaInicioAsc(
            Long institucionId, Long periodoId, Long materiaId);

    Optional<ProyectoEstudiantil> findByIdAndInstitucionId(Long id, Long institucionId);
}
