package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.MiembroProyecto;

public interface MiembroProyectoRepository extends JpaRepository<MiembroProyecto, Long> {

    List<MiembroProyecto> findByProyectoIdOrderByFechaAsignacionDesc(Long proyectoId);

    boolean existsByProyectoIdAndUsuarioId(Long proyectoId, Long usuarioId);

    java.util.Optional<MiembroProyecto> findByIdAndProyectoId(Long id, Long proyectoId);

    void deleteByProyectoId(Long proyectoId);

    List<MiembroProyecto> findByUsuarioId(Long usuarioId);
}
