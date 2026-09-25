package com.chavescr.nexa.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AsistenciaEstudiante;

public interface AsistenciaEstudianteRepository extends JpaRepository<AsistenciaEstudiante, Long> {

    List<AsistenciaEstudiante> findByDireccionIdAndNivelAcademicoIdAndFechaAndMateriaIdAndNumeroLeccion(
            Long direccionId, Long nivelId, LocalDate fecha, Long materiaId, Integer numeroLeccion);

    Optional<AsistenciaEstudiante> findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
            Long direccionId, Long estudianteId, LocalDate fecha, Long materiaId, Integer numeroLeccion);

    List<AsistenciaEstudiante> findByDireccionIdAndEstudianteIdAndMateriaIdAndFechaBetween(Long direccionId,
            Long estudianteId, Long materiaId, LocalDate desde, LocalDate hasta);
}
