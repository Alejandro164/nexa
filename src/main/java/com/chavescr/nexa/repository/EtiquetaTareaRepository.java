package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.EtiquetaTarea;

public interface EtiquetaTareaRepository extends JpaRepository<EtiquetaTarea, Long> {

    List<EtiquetaTarea> findByDireccionIdOrderByNombreAsc(Long direccionId);

    Optional<EtiquetaTarea> findByIdAndDireccionId(Long id, Long direccionId);

    boolean existsByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);
}
