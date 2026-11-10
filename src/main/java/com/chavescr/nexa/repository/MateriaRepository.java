package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.ComponenteEvaluacion;
import com.chavescr.nexa.entity.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    @EntityGraph(attributePaths = { "tipoMateria", "componenteEvaluacion" })
    List<Materia> findByDireccionIdOrderByNombreAsc(Long direccionId);

    @EntityGraph(attributePaths = "tipoMateria")
    List<Materia> findByDireccionIdAndActivoTrueOrderByNombreAsc(Long direccionId);

    List<Materia> findByDireccionIdAndActivoTrueAndTipoMateriaIdOrderByNombreAsc(
            Long direccionId, Long tipoMateriaId);

    @EntityGraph(attributePaths = { "tipoMateria", "componenteEvaluacion" })
    Optional<Materia> findByIdAndDireccionId(Long id, Long direccionId);

    boolean existsByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);

    boolean existsByTipoMateriaId(Long tipoMateriaId);

    boolean existsByComponenteEvaluacionId(Long componenteEvaluacionId);

    /** Filas [componenteEvaluacionId, cantidad de materias] de una dirección. */
    @Query("SELECT m.componenteEvaluacion.id, COUNT(m) FROM Materia m "
            + "WHERE m.direccion.id = :direccionId AND m.componenteEvaluacion IS NOT NULL "
            + "GROUP BY m.componenteEvaluacion.id")
    List<Object[]> contarPorComponente(@Param("direccionId") Long direccionId);

    /** Asigna un componente a las materias de la dirección que todavía no tienen uno. */
    @Modifying
    @Query("UPDATE Materia m SET m.componenteEvaluacion = :componente "
            + "WHERE m.direccion.id = :direccionId AND m.componenteEvaluacion IS NULL")
    int asignarComponenteFaltante(@Param("direccionId") Long direccionId,
            @Param("componente") ComponenteEvaluacion componente);

    /** Mantiene la columna heredada {@code materias.tipo} igual al nombre del tipo. */
    @Modifying
    @Query("UPDATE Materia m SET m.tipo = :nombre WHERE m.tipoMateria.id = :tipoMateriaId")
    int sincronizarNombreTipo(@Param("tipoMateriaId") Long tipoMateriaId, @Param("nombre") String nombre);
}
