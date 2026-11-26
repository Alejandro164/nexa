package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.EstadoMatricula;
import com.chavescr.nexa.entity.MatriculaEstudiante;

@Repository
public interface MatriculaEstudianteRepository extends JpaRepository<MatriculaEstudiante, Long> {

    @Query("SELECT m FROM MatriculaEstudiante m LEFT JOIN FETCH m.nivelAcademico "
            + "WHERE m.direccion.id = :direccionId AND m.estado = :estado "
            + "ORDER BY m.nombre ASC")
    List<MatriculaEstudiante> findAllByDireccionIdAndEstado(@Param("direccionId") Long direccionId,
            @Param("estado") EstadoMatricula estado);

    @Query("SELECT m FROM MatriculaEstudiante m LEFT JOIN FETCH m.nivelAcademico "
            + "WHERE m.id = :id AND m.direccion.id = :direccionId")
    Optional<MatriculaEstudiante> findByIdAndDireccionId(@Param("id") Long id,
            @Param("direccionId") Long direccionId);

    @Query("SELECT m FROM MatriculaEstudiante m LEFT JOIN FETCH m.nivelAcademico "
            + "WHERE m.direccion.id = :direccionId AND m.estado IN :estados")
    List<MatriculaEstudiante> findAllByDireccionIdAndEstadoIn(@Param("direccionId") Long direccionId,
            @Param("estados") Collection<EstadoMatricula> estados);

    long countByDireccionId(Long direccionId);
}
