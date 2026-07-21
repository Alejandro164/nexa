package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TareaEstudiantil;

public interface TareaEstudiantilRepository extends JpaRepository<TareaEstudiantil, Long> {
    List<TareaEstudiantil> findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaEntregaAsc(
            Long institucionId, Long periodoId, Long materiaId);

    Optional<TareaEstudiantil> findByIdAndInstitucionId(Long id, Long institucionId);
}
