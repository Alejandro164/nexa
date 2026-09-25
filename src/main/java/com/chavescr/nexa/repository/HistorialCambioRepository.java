package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.HistorialCambio;

public interface HistorialCambioRepository extends JpaRepository<HistorialCambio, Long> {
    List<HistorialCambio> findByDireccionIdAndNivelIdAndMateriaIdOrderByFechaDesc(
            Long direccionId, Long nivelId, Long materiaId);

    List<HistorialCambio> findByDireccionIdOrderByFechaDesc(Long direccionId);

    List<HistorialCambio> findTop8ByDireccionIdOrderByFechaDesc(Long direccionId);
}
