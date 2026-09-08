package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    @EntityGraph(attributePaths = "tipoMateria")
    List<Materia> findByInstitucionIdOrderByNombreAsc(Long institucionId);

    @EntityGraph(attributePaths = "tipoMateria")
    List<Materia> findByInstitucionIdAndActivoTrueOrderByNombreAsc(Long institucionId);

    @EntityGraph(attributePaths = "tipoMateria")
    Optional<Materia> findByIdAndInstitucionId(Long id, Long institucionId);
}
