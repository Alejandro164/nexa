package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ProyectoDefinicion;

public interface ProyectoDefinicionRepository extends JpaRepository<ProyectoDefinicion, Long> {
    List<ProyectoDefinicion> findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
            Long institucionId, Long nivelId, Long materiaId, Long periodoId);

    Optional<ProyectoDefinicion> findByIdAndInstitucionId(Long id, Long institucionId);
}
