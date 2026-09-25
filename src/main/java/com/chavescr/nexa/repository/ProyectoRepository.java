package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Proyecto;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    List<Proyecto> findByDireccionIdOrderByFechaCreacionDesc(Long direccionId);

    java.util.Optional<Proyecto> findByIdAndDireccionId(Long id, Long direccionId);
}
