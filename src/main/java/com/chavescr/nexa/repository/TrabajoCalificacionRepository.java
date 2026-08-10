package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TrabajoCalificacion;

public interface TrabajoCalificacionRepository extends JpaRepository<TrabajoCalificacion, Long> {
    List<TrabajoCalificacion> findByTrabajoDefinicionId(Long trabajoDefinicionId);

    List<TrabajoCalificacion> findByTrabajoDefinicionIdIn(List<Long> trabajoDefinicionIds);

    Optional<TrabajoCalificacion> findByTrabajoDefinicionIdAndEstudianteId(Long trabajoDefinicionId, Long estudianteId);

    boolean existsByTrabajoDefinicionId(Long trabajoDefinicionId);

    List<TrabajoCalificacion> findByTrabajoDefinicion_PeriodoIdAndTrabajoDefinicion_MateriaId(Long periodoId, Long materiaId);
}
