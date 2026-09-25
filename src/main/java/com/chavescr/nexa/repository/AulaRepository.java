package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Aula;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    @EntityGraph(attributePaths = "tipoAula")
    List<Aula> findByDireccionIdOrderByNombreAsc(Long direccionId);

    @EntityGraph(attributePaths = "tipoAula")
    List<Aula> findByDireccionIdAndActivoTrueOrderByNombreAsc(Long direccionId);

    @EntityGraph(attributePaths = "tipoAula")
    Optional<Aula> findByIdAndDireccionId(Long id, Long direccionId);
}
