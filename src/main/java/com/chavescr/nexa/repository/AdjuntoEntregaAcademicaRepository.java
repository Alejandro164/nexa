package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AdjuntoEntregaAcademica;

public interface AdjuntoEntregaAcademicaRepository extends JpaRepository<AdjuntoEntregaAcademica, Long> {

    Optional<AdjuntoEntregaAcademica> findByIdAndPresentacion_Entrega_Direccion_Id(Long id, Long direccionId);
}
