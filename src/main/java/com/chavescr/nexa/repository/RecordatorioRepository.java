package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.Recordatorio;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    List<Recordatorio> findByUsuarioIdAndDireccionIdOrderByFechaLimiteAsc(Long usuarioId, Long direccionId);

    Optional<Recordatorio> findByIdAndUsuarioIdAndDireccionId(Long id, Long usuarioId, Long direccionId);
}
