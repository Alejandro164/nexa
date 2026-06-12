package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    List<Materia> findByInstitucionIdOrderByNombreAsc(Long institucionId);
    List<Materia> findByInstitucionIdAndActivoTrueOrderByNombreAsc(Long institucionId);
    Optional<Materia> findByIdAndInstitucionId(Long id, Long institucionId);
}
