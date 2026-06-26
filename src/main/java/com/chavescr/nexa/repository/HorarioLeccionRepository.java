package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.HorarioLeccion;

public interface HorarioLeccionRepository extends JpaRepository<HorarioLeccion, Long> {
    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndNivelIdOrderByNumeroLeccionAsc(
            Long institucionId, Long periodoId, Long nivelId);

    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccionOrderByIdAsc(
            Long institucionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    Optional<HorarioLeccion> findByIdAndInstitucionId(Long id, Long institucionId);

    void deleteByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);
    void deleteByInstitucionIdAndNivelId(Long institucionId, Long nivelId);
    void deleteByInstitucionIdAndMateriaId(Long institucionId, Long materiaId);
}
