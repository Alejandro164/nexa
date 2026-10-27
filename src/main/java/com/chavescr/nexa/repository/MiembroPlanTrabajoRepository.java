package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.MiembroPlanTrabajo;

public interface MiembroPlanTrabajoRepository extends JpaRepository<MiembroPlanTrabajo, Long> {

    List<MiembroPlanTrabajo> findByPlanTrabajoIdOrderByFechaAsignacionDesc(Long planTrabajoId);

    boolean existsByPlanTrabajoIdAndUsuarioId(Long planTrabajoId, Long usuarioId);

    java.util.Optional<MiembroPlanTrabajo> findByIdAndPlanTrabajoId(Long id, Long planTrabajoId);

    void deleteByPlanTrabajoId(Long planTrabajoId);

    List<MiembroPlanTrabajo> findByUsuarioId(Long usuarioId);
}
