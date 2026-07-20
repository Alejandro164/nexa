package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.IndicadorCotidiano;

public interface IndicadorCotidianoRepository extends JpaRepository<IndicadorCotidiano, Long> {
    List<IndicadorCotidiano> findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByIdAsc(
            Long institucionId, Long periodoId, Long materiaId);

    Optional<IndicadorCotidiano> findByIdAndInstitucionId(Long id, Long institucionId);
}
