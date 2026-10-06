package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.SubgrupoEstudiante;

public interface SubgrupoEstudianteRepository extends JpaRepository<SubgrupoEstudiante, Long> {

    @Query("SELECT s.estudiante.id, s.leccion.id FROM SubgrupoEstudiante s "
            + "WHERE s.leccion.direccion.id = :direccionId AND s.leccion.periodo.id = :periodoId "
            + "AND s.leccion.nivel.id = :nivelId")
    List<Object[]> findEstudianteYLeccion(
            @Param("direccionId") Long direccionId,
            @Param("periodoId") Long periodoId,
            @Param("nivelId") Long nivelId);

    @Query("SELECT s.leccion.id FROM SubgrupoEstudiante s WHERE s.estudiante.id = :estudianteId")
    List<Long> findLeccionIdsByEstudianteId(@Param("estudianteId") Long estudianteId);

    @Query("SELECT s.estudiante.id FROM SubgrupoEstudiante s WHERE s.leccion.id IN :leccionIds")
    List<Long> findEstudianteIdsByLeccionIdIn(@Param("leccionIds") Collection<Long> leccionIds);

    boolean existsByEstudiante_IdAndLeccion_Direccion_IdAndLeccion_Periodo_IdAndLeccion_Nivel_IdAndLeccion_Materia_Id(
            Long estudianteId, Long direccionId, Long periodoId, Long nivelId, Long materiaId);

    void deleteByEstudiante_IdAndLeccion_IdIn(Long estudianteId, Collection<Long> leccionIds);
}
