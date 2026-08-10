package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.HistorialCambio;

public interface HistorialCambioRepository extends JpaRepository<HistorialCambio, Long> {
    List<HistorialCambio> findByInstitucionIdAndNivelIdAndMateriaIdOrderByFechaDesc(
            Long institucionId, Long nivelId, Long materiaId);
}
