package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Oficio;

public interface OficioRepository extends JpaRepository<Oficio, Long> {
    List<Oficio> findByInstitucionIdOrderByFechaDesc(Long institucionId);

    Optional<Oficio> findByIdAndInstitucionId(Long id, Long institucionId);

    long countByInstitucionIdAndNumeroStartingWith(Long institucionId, String prefijo);
}
