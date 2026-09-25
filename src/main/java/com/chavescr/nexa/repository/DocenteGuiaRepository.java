package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.DocenteGuia;

public interface DocenteGuiaRepository extends JpaRepository<DocenteGuia, Long> {

    @EntityGraph(attributePaths = "nivel")
    List<DocenteGuia> findByDireccionIdAndDocenteIdOrderByNivel_GradoAscNivel_SeccionAsc(
            Long direccionId, Long docenteId);

    @EntityGraph(attributePaths = { "docente", "nivel" })
    List<DocenteGuia> findByDireccionIdOrderByNivel_GradoAscNivel_SeccionAsc(Long direccionId);

    @EntityGraph(attributePaths = "docente")
    Optional<DocenteGuia> findByDireccionIdAndNivelId(Long direccionId, Long nivelId);

    void deleteByDireccionIdAndDocenteId(Long direccionId, Long docenteId);

    void deleteByDocenteId(Long docenteId);

    void deleteByDireccionIdAndNivelId(Long direccionId, Long nivelId);
}
