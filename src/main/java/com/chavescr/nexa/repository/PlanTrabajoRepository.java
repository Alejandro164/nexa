package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.PlanTrabajo;

public interface PlanTrabajoRepository extends JpaRepository<PlanTrabajo, Long> {

    List<PlanTrabajo> findByDireccionIdOrderByFechaCreacionDesc(Long direccionId);

    java.util.Optional<PlanTrabajo> findByIdAndDireccionId(Long id, Long direccionId);
}
