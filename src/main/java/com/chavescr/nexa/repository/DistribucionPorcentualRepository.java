package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.DistribucionPorcentual;

public interface DistribucionPorcentualRepository extends JpaRepository<DistribucionPorcentual, Long> {
    Optional<DistribucionPorcentual> findByDireccionIdAndPeriodoIdAndMateriaId(
            Long direccionId, Long periodoId, Long materiaId);

    @Query("""
            SELECT d FROM DistribucionPorcentual d
            JOIN FETCH d.materia
            JOIN FETCH d.periodo
            WHERE d.direccion.id = :direccionId AND d.periodo.id IN :periodoIds
            """)
    List<DistribucionPorcentual> findByDireccionIdAndPeriodoIdIn(@Param("direccionId") Long direccionId,
            @Param("periodoIds") Collection<Long> periodoIds);
}
