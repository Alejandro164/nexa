package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.BitacoraEvento;
import com.chavescr.nexa.entity.ModuloSistema;

public interface BitacoraEventoRepository extends JpaRepository<BitacoraEvento, Long> {

    List<BitacoraEvento> findByDireccionIdOrderByFechaDesc(Long direccionId, Pageable pageable);

    List<BitacoraEvento> findByDireccionIdAndModuloOrderByFechaDesc(Long direccionId, ModuloSistema modulo,
            Pageable pageable);

    List<BitacoraEvento> findByDireccionIdAndAccionOrderByFechaDesc(Long direccionId, AccionHistorial accion,
            Pageable pageable);

    List<BitacoraEvento> findByDireccionIdAndModuloAndAccionOrderByFechaDesc(Long direccionId,
            ModuloSistema modulo, AccionHistorial accion, Pageable pageable);
}
