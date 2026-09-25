package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.TareaProyecto;

public interface TareaProyectoRepository extends JpaRepository<TareaProyecto, Long> {

    List<TareaProyecto> findByMiembroIdOrderByFechaLimiteAsc(Long miembroId);

    List<TareaProyecto> findByProyectoIdOrderByFechaLimiteAsc(Long proyectoId);

    Optional<TareaProyecto> findByIdAndProyectoId(Long id, Long proyectoId);

    void deleteByMiembroId(Long miembroId);

    @Query("SELECT t FROM TareaProyecto t JOIN t.proyecto p WHERE p.direccion.id = :direccionId ORDER BY t.fechaLimite ASC")
    List<TareaProyecto> findByDireccionIdOrderByFechaLimiteAsc(@Param("direccionId") Long direccionId);

    Optional<TareaProyecto> findByIdAndProyecto_Direccion_Id(Long id, Long direccionId);
}
