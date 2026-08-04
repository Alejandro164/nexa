package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ProyectoCalificacion;

public interface ProyectoCalificacionRepository extends JpaRepository<ProyectoCalificacion, Long> {
    List<ProyectoCalificacion> findByProyectoDefinicionId(Long proyectoDefinicionId);

    Optional<ProyectoCalificacion> findByProyectoDefinicionIdAndEstudianteId(Long proyectoDefinicionId, Long estudianteId);

    List<ProyectoCalificacion> findByProyectoDefinicion_PeriodoIdAndProyectoDefinicion_MateriaId(Long periodoId, Long materiaId);
}
