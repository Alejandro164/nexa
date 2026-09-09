package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.DocenteGuia;

public interface DocenteGuiaRepository extends JpaRepository<DocenteGuia, Long> {

    @EntityGraph(attributePaths = "nivel")
    List<DocenteGuia> findByInstitucionIdAndDocenteIdOrderByNivel_GradoAscNivel_SeccionAsc(
            Long institucionId, Long docenteId);

    @EntityGraph(attributePaths = { "docente", "nivel" })
    List<DocenteGuia> findByInstitucionIdOrderByNivel_GradoAscNivel_SeccionAsc(Long institucionId);

    @EntityGraph(attributePaths = "docente")
    Optional<DocenteGuia> findByInstitucionIdAndNivelId(Long institucionId, Long nivelId);

    void deleteByInstitucionIdAndDocenteId(Long institucionId, Long docenteId);

    void deleteByDocenteId(Long docenteId);

    void deleteByInstitucionIdAndNivelId(Long institucionId, Long nivelId);
}
