package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.EnvioNotasDocente;

public interface EnvioNotasDocenteRepository extends JpaRepository<EnvioNotasDocente, Long> {

    Optional<EnvioNotasDocente> findByInstitucionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
            Long institucionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId);

    boolean existsByInstitucionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
            Long institucionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId);

    void deleteByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);
}
