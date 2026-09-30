package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.ClassroomEntrega;
import com.chavescr.nexa.entity.EstadoEntrega;

@Repository
public interface ClassroomEntregaRepository extends JpaRepository<ClassroomEntrega, Long> {

    Optional<ClassroomEntrega> findByPublicacionIdAndEstudianteId(Long publicacionId, Long estudianteId);

    List<ClassroomEntrega> findByPublicacionId(Long publicacionId);

    List<ClassroomEntrega> findByPublicacionClaseIdAndEstudianteId(Long claseId, Long estudianteId);

    // Entregas pendientes de revisar por el docente, en toda la clase
    long countByPublicacionClaseIdAndEstado(Long claseId, EstadoEntrega estado);
}
