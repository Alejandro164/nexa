package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Proyecto;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    List<Proyecto> findByInstitucionIdOrderByFechaCreacionDesc(Long institucionId);

    java.util.Optional<Proyecto> findByIdAndInstitucionId(Long id, Long institucionId);
}
