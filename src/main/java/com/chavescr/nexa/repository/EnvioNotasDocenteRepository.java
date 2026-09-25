package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.EnvioNotasDocente;

public interface EnvioNotasDocenteRepository extends JpaRepository<EnvioNotasDocente, Long> {

    Optional<EnvioNotasDocente> findByDireccionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
            Long direccionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId);

    boolean existsByDireccionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
            Long direccionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId);

    void deleteByDireccionIdAndPeriodoId(Long direccionId, Long periodoId);
}
