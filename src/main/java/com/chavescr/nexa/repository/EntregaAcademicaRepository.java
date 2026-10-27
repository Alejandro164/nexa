package com.chavescr.nexa.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.EntregaAcademica;

public interface EntregaAcademicaRepository extends JpaRepository<EntregaAcademica, Long> {

    List<EntregaAcademica> findByDireccionIdOrderByFechaInicioDescNombreAsc(Long direccionId);

    /** Entregas activas cuyo plazo ya comenzó: son las que los docentes ven como pendientes. */
    List<EntregaAcademica> findByDireccionIdAndActivaTrueAndFechaInicioLessThanEqualOrderByFechaFinAsc(
            Long direccionId, LocalDate hoy);

    Optional<EntregaAcademica> findByIdAndDireccionId(Long id, Long direccionId);
}
