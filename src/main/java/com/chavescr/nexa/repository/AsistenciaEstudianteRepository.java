package com.chavescr.nexa.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AsistenciaEstudiante;

public interface AsistenciaEstudianteRepository extends JpaRepository<AsistenciaEstudiante, Long> {

    List<AsistenciaEstudiante> findByInstitucionIdAndNivelAcademicoIdAndFecha(Long institucionId, Long nivelId,
            LocalDate fecha);

    Optional<AsistenciaEstudiante> findByInstitucionIdAndEstudianteIdAndFecha(Long institucionId, Long estudianteId,
            LocalDate fecha);

    List<AsistenciaEstudiante> findByInstitucionIdAndEstudianteIdAndFechaBetween(Long institucionId,
            Long estudianteId, LocalDate desde, LocalDate hasta);
}
