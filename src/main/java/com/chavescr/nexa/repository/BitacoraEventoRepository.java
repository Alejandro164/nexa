package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.BitacoraEvento;
import com.chavescr.nexa.entity.ModuloSistema;

public interface BitacoraEventoRepository extends JpaRepository<BitacoraEvento, Long> {

    List<BitacoraEvento> findByInstitucionIdOrderByFechaDesc(Long institucionId, Pageable pageable);

    List<BitacoraEvento> findByInstitucionIdAndModuloOrderByFechaDesc(Long institucionId, ModuloSistema modulo,
            Pageable pageable);

    List<BitacoraEvento> findByInstitucionIdAndAccionOrderByFechaDesc(Long institucionId, AccionHistorial accion,
            Pageable pageable);

    List<BitacoraEvento> findByInstitucionIdAndModuloAndAccionOrderByFechaDesc(Long institucionId,
            ModuloSistema modulo, AccionHistorial accion, Pageable pageable);
}
