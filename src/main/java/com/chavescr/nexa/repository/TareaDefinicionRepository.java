package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TareaDefinicion;

public interface TareaDefinicionRepository extends JpaRepository<TareaDefinicion, Long> {
    List<TareaDefinicion> findByInstitucionIdAndNivelIdAndMateriaIdOrderByFechaEntregaAsc(
            Long institucionId, Long nivelId, Long materiaId);

    Optional<TareaDefinicion> findByIdAndInstitucionId(Long id, Long institucionId);
}
