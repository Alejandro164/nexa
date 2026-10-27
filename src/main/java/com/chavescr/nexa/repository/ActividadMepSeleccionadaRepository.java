package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ActividadMepSeleccionada;

public interface ActividadMepSeleccionadaRepository extends JpaRepository<ActividadMepSeleccionada, Long> {

    List<ActividadMepSeleccionada> findByDireccionIdAndAnioIn(Long direccionId, List<Integer> anios);

    Optional<ActividadMepSeleccionada> findByDireccionIdAndAnioAndMepEventoId(Long direccionId, int anio, String mepEventoId);

    void deleteByDireccionIdAndAnioAndMepEventoId(Long direccionId, int anio, String mepEventoId);
}
