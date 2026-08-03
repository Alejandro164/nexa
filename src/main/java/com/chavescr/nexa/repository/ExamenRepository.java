package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Examen;

public interface ExamenRepository extends JpaRepository<Examen, Long> {
    List<Examen> findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
            Long institucionId, Long nivelId, Long materiaId, Long periodoId);

    Optional<Examen> findByIdAndInstitucionId(Long id, Long institucionId);
}
