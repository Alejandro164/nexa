package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.DocenteMateria;

public interface DocenteMateriaRepository extends JpaRepository<DocenteMateria, Long> {

    @EntityGraph(attributePaths = "materia")
    List<DocenteMateria> findByDireccionIdAndDocenteIdOrderByMateria_NombreAsc(
            Long direccionId, Long docenteId);

    @EntityGraph(attributePaths = { "docente", "materia" })
    List<DocenteMateria> findByDireccionIdOrderByMateria_NombreAsc(Long direccionId);

    @EntityGraph(attributePaths = "docente")
    List<DocenteMateria> findByDireccionIdAndMateriaIdOrderByDocente_NombreAsc(
            Long direccionId, Long materiaId);

    boolean existsByDireccionIdAndDocenteIdAndMateriaId(Long direccionId, Long docenteId, Long materiaId);

    void deleteByDireccionIdAndDocenteId(Long direccionId, Long docenteId);

    void deleteByDocenteId(Long docenteId);

    void deleteByDireccionIdAndMateriaId(Long direccionId, Long materiaId);
}
