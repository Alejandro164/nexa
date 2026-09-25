package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.BloqueoLeccion;

public interface BloqueoLeccionRepository extends JpaRepository<BloqueoLeccion, Long> {

    @EntityGraph(attributePaths = "tipoMateria")
    List<BloqueoLeccion> findByDireccionIdOrderByIdAsc(Long direccionId);

    void deleteByDireccionId(Long direccionId);
}
