package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Oficio;

public interface OficioRepository extends JpaRepository<Oficio, Long> {
    List<Oficio> findByDireccionIdOrderByFechaDesc(Long direccionId);

    Optional<Oficio> findByIdAndDireccionId(Long id, Long direccionId);

    long countByDireccionIdAndNumeroStartingWith(Long direccionId, String prefijo);
}
