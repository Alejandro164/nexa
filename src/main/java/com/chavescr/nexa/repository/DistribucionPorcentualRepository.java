package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.DistribucionPorcentual;

public interface DistribucionPorcentualRepository extends JpaRepository<DistribucionPorcentual, Long> {
    Optional<DistribucionPorcentual> findByInstitucionIdAndPeriodoIdAndMateriaId(
            Long institucionId, Long periodoId, Long materiaId);
}
