package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.SeccionBloqueoLeccion;

public interface SeccionBloqueoLeccionRepository extends JpaRepository<SeccionBloqueoLeccion, Long> {

    List<SeccionBloqueoLeccion> findByDireccionIdAndPeriodoIdAndNivelId(
            Long direccionId, Long periodoId, Long nivelId);

    Optional<SeccionBloqueoLeccion> findByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
            Long direccionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    void deleteByDireccionIdAndNivelId(Long direccionId, Long nivelId);

    void deleteByDireccionIdAndPeriodoId(Long direccionId, Long periodoId);
}
