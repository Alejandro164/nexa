package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.DocenteMateria;

public interface DocenteMateriaRepository extends JpaRepository<DocenteMateria, Long> {

    @EntityGraph(attributePaths = "materia")
    List<DocenteMateria> findByInstitucionIdAndDocenteIdOrderByMateria_NombreAsc(
            Long institucionId, Long docenteId);

    @EntityGraph(attributePaths = { "docente", "materia" })
    List<DocenteMateria> findByInstitucionIdOrderByMateria_NombreAsc(Long institucionId);

    @EntityGraph(attributePaths = "docente")
    List<DocenteMateria> findByInstitucionIdAndMateriaIdOrderByDocente_NombreAsc(
            Long institucionId, Long materiaId);

    boolean existsByInstitucionIdAndDocenteIdAndMateriaId(Long institucionId, Long docenteId, Long materiaId);

    void deleteByInstitucionIdAndDocenteId(Long institucionId, Long docenteId);

    void deleteByDocenteId(Long docenteId);

    void deleteByInstitucionIdAndMateriaId(Long institucionId, Long materiaId);
}
