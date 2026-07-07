package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Aula;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findByInstitucionIdOrderByNombreAsc(Long institucionId);
    List<Aula> findByInstitucionIdAndActivoTrueOrderByNombreAsc(Long institucionId);
    Optional<Aula> findByIdAndInstitucionId(Long id, Long institucionId);
}
