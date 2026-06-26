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

    @Query("SELECT t FROM TareaProyecto t JOIN t.proyecto p WHERE p.institucion.id = :institucionId ORDER BY t.fechaLimite ASC")
    List<TareaProyecto> findByInstitucionIdOrderByFechaLimiteAsc(@Param("institucionId") Long institucionId);

    Optional<TareaProyecto> findByIdAndProyecto_Institucion_Id(Long id, Long institucionId);
}
