package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    @EntityGraph(attributePaths = "tipoMateria")
    List<Materia> findByDireccionIdOrderByNombreAsc(Long direccionId);

    @EntityGraph(attributePaths = "tipoMateria")
    List<Materia> findByDireccionIdAndActivoTrueOrderByNombreAsc(Long direccionId);

    @EntityGraph(attributePaths = "tipoMateria")
    Optional<Materia> findByIdAndDireccionId(Long id, Long direccionId);

    boolean existsByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);
}
