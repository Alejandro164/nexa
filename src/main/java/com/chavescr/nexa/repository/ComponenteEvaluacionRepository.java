package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ComponenteEvaluacion;

public interface ComponenteEvaluacionRepository extends JpaRepository<ComponenteEvaluacion, Long> {
    List<ComponenteEvaluacion> findByDireccionIdOrderByOrdenAscNombreAsc(Long direccionId);

    List<ComponenteEvaluacion> findByDireccionIdAndActivoTrueOrderByOrdenAscNombreAsc(Long direccionId);

    Optional<ComponenteEvaluacion> findByIdAndDireccionId(Long id, Long direccionId);

    Optional<ComponenteEvaluacion> findFirstByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);

    Optional<ComponenteEvaluacion> findFirstByDireccionIdOrderByOrdenAscNombreAsc(Long direccionId);

    boolean existsByDireccionId(Long direccionId);

    boolean existsByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);

    boolean existsByDireccionIdAndNombreIgnoreCaseAndIdNot(Long direccionId, String nombre, Long id);
}
