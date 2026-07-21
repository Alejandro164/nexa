package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TrabajoExtraclase;

public interface TrabajoExtraclaseRepository extends JpaRepository<TrabajoExtraclase, Long> {
    List<TrabajoExtraclase> findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaEntregaAsc(
            Long institucionId, Long periodoId, Long materiaId);

    Optional<TrabajoExtraclase> findByIdAndInstitucionId(Long id, Long institucionId);
}
