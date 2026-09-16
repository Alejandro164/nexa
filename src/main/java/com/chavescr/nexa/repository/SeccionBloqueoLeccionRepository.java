package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.SeccionBloqueoLeccion;

public interface SeccionBloqueoLeccionRepository extends JpaRepository<SeccionBloqueoLeccion, Long> {

    List<SeccionBloqueoLeccion> findByInstitucionIdAndPeriodoIdAndNivelId(
            Long institucionId, Long periodoId, Long nivelId);

    Optional<SeccionBloqueoLeccion> findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
            Long institucionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    void deleteByInstitucionIdAndNivelId(Long institucionId, Long nivelId);

    void deleteByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);
}
