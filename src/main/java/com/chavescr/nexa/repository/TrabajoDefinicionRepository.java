package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TrabajoDefinicion;

public interface TrabajoDefinicionRepository extends JpaRepository<TrabajoDefinicion, Long> {
    List<TrabajoDefinicion> findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
            Long institucionId, Long nivelId, Long materiaId, Long periodoId);

    Optional<TrabajoDefinicion> findByIdAndInstitucionId(Long id, Long institucionId);
}
