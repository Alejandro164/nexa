package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AdjuntoTarea;

public interface AdjuntoTareaRepository extends JpaRepository<AdjuntoTarea, Long> {

    Optional<AdjuntoTarea> findByIdAndTarea_PlanTrabajo_Direccion_Id(Long id, Long direccionId);
}
