package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Aula;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    @EntityGraph(attributePaths = "tipoAula")
    List<Aula> findByInstitucionIdOrderByNombreAsc(Long institucionId);

    @EntityGraph(attributePaths = "tipoAula")
    List<Aula> findByInstitucionIdAndActivoTrueOrderByNombreAsc(Long institucionId);

    @EntityGraph(attributePaths = "tipoAula")
    Optional<Aula> findByIdAndInstitucionId(Long id, Long institucionId);
}
