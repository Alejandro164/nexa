package com.chavescr.nexa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.RetiroEstudiante;

@Repository
public interface RetiroEstudianteRepository extends JpaRepository<RetiroEstudiante, Long> {

    List<RetiroEstudiante> findByInstitucionIdAndFechaHoraSolicitudBetweenOrderByFechaHoraSolicitudDesc(
            Long institucionId, LocalDateTime inicio, LocalDateTime fin);

    List<RetiroEstudiante> findByPadreIdOrderByFechaHoraSolicitudDesc(Long padreId);
}
