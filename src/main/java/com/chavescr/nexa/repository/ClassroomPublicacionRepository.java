package com.chavescr.nexa.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.ClassroomPublicacion;
import com.chavescr.nexa.entity.TipoPublicacion;

@Repository
public interface ClassroomPublicacionRepository extends JpaRepository<ClassroomPublicacion, Long> {

    // Tablón: todo lo publicado en la clase, más reciente primero
    @EntityGraph(attributePaths = { "autor" })
    List<ClassroomPublicacion> findByClaseIdOrderByFechaCreacionDesc(Long claseId);

    @EntityGraph(attributePaths = { "autor" })
    List<ClassroomPublicacion> findByClaseIdAndTipoOrderByFechaCreacionDesc(Long claseId, TipoPublicacion tipo);

    Optional<ClassroomPublicacion> findByIdAndClaseId(Long id, Long claseId);

    // Próximas entregas (tarjetas de clase del estudiante)
    List<ClassroomPublicacion> findByClaseIdAndTipoAndFechaEntregaAfterOrderByFechaEntregaAsc(Long claseId,
            TipoPublicacion tipo, LocalDateTime desde);
}
