package com.chavescr.nexa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.RegistroAsistencia;

@Repository
public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    List<RegistroAsistencia> findByInstitucionIdOrderByFechaHoraDesc(Long institucionId);

    List<RegistroAsistencia> findByInstitucionIdAndFechaHoraBetweenOrderByFechaHoraDesc(
            Long institucionId, LocalDateTime inicio, LocalDateTime fin);

    List<RegistroAsistencia> findByUsuarioIdAndInstitucionIdOrderByFechaHoraDesc(
            Long usuarioId, Long institucionId);
}
