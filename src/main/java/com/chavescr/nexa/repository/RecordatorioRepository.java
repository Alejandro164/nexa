package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Recordatorio;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    List<Recordatorio> findByUsuarioIdAndInstitucionIdOrderByFechaLimiteAsc(Long usuarioId, Long institucionId);

    Optional<Recordatorio> findByIdAndUsuarioIdAndInstitucionId(Long id, Long usuarioId, Long institucionId);
}
