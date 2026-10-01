package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.ObservacionGuia;

public interface ObservacionGuiaRepository extends JpaRepository<ObservacionGuia, Long> {

    Optional<ObservacionGuia> findByDireccionIdAndEstudianteIdAndPeriodoId(Long direccionId, Long estudianteId,
            Long periodoId);

    @Query("""
            SELECT o.estudiante.id, o.periodo.id, o.texto
            FROM ObservacionGuia o
            WHERE o.direccion.id = :direccionId
              AND o.estudiante.id IN :estudianteIds
              AND o.periodo.id IN :periodoIds
            """)
    List<Object[]> findTextos(@Param("direccionId") Long direccionId,
            @Param("estudianteIds") Collection<Long> estudianteIds,
            @Param("periodoIds") Collection<Long> periodoIds);
}
