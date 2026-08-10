package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.NotaExamen;

public interface NotaExamenRepository extends JpaRepository<NotaExamen, Long> {
    List<NotaExamen> findByExamenId(Long examenId);

    List<NotaExamen> findByExamenIdIn(List<Long> examenIds);

    Optional<NotaExamen> findByExamenIdAndEstudianteId(Long examenId, Long estudianteId);

    boolean existsByExamenId(Long examenId);

    List<NotaExamen> findByEstudianteIdAndExamen_PeriodoIdAndExamen_MateriaId(
            Long estudianteId, Long periodoId, Long materiaId);

    List<NotaExamen> findByExamen_PeriodoIdAndExamen_MateriaId(Long periodoId, Long materiaId);
}
