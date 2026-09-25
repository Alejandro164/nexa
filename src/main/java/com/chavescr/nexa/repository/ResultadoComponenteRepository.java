package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ResultadoComponente;

public interface ResultadoComponenteRepository extends JpaRepository<ResultadoComponente, Long> {

    List<ResultadoComponente> findByComponenteIdIn(List<Long> componenteIds);

    List<ResultadoComponente> findByComponenteIdInAndPeriodoId(List<Long> componenteIds, Long periodoId);

    List<ResultadoComponente> findByComponenteIdAndPeriodoId(Long componenteId, Long periodoId);

    Optional<ResultadoComponente> findByComponenteIdAndEstudianteIdAndPeriodoId(Long componenteId, Long estudianteId,
            Long periodoId);

    boolean existsByComponenteId(Long componenteId);

    List<ResultadoComponente> findByComponente_Direccion_IdAndComponente_Nivel_IdAndComponente_Materia_IdAndPeriodo_Id(
            Long direccionId, Long nivelId, Long materiaId, Long periodoId);
}
