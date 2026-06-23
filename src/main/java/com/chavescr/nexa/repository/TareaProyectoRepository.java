package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TareaProyecto;

public interface TareaProyectoRepository extends JpaRepository<TareaProyecto, Long> {

    List<TareaProyecto> findByMiembroIdOrderByFechaLimiteAsc(Long miembroId);

    List<TareaProyecto> findByProyectoIdOrderByFechaLimiteAsc(Long proyectoId);

    java.util.Optional<TareaProyecto> findByIdAndProyectoId(Long id, Long proyectoId);

    void deleteByMiembroId(Long miembroId);
}
