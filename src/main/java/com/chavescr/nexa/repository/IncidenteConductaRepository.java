package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;

public interface IncidenteConductaRepository extends JpaRepository<IncidenteConducta, Long> {

    List<IncidenteConducta> findByDireccionIdAndPeriodoIdAndEstudianteId(
            Long direccionId, Long periodoId, Long estudianteId);

    @Query("SELECT i.estudiante.id, i FROM IncidenteConducta i "
            + "WHERE i.direccion.id = :direccionId AND i.periodo.id = :periodoId "
            + "AND i.estudiante.id IN :estudianteIds")
    List<Object[]> findDeEstudiantes(@Param("direccionId") Long direccionId,
            @Param("periodoId") Long periodoId, @Param("estudianteIds") Collection<Long> estudianteIds);

    @Query("SELECT DISTINCT i FROM IncidenteConducta i "
            + "JOIN FETCH i.estudiante e "
            + "LEFT JOIN FETCH e.nivelAcademico n "
            + "LEFT JOIN FETCH i.registradoPor "
            + "WHERE i.direccion.id = :direccionId AND i.periodo.id = :periodoId "
            + "AND i.tipo = :tipo "
            + "AND (:grado IS NULL OR n.grado = :grado) "
            + "AND (:nivelId IS NULL OR n.id = :nivelId) "
            + "ORDER BY i.fecha DESC, i.id DESC")
    List<IncidenteConducta> findDelPeriodo(@Param("direccionId") Long direccionId,
            @Param("periodoId") Long periodoId, @Param("tipo") TipoIncidente tipo,
            @Param("grado") Integer grado, @Param("nivelId") Long nivelId);

    @Query("SELECT DISTINCT i FROM IncidenteConducta i "
            + "JOIN FETCH i.estudiante e "
            + "JOIN FETCH e.nivelAcademico n "
            + "LEFT JOIN FETCH i.registradoPor "
            + "WHERE i.direccion.id = :direccionId AND i.periodo.id = :periodoId "
            + "AND i.tipo = :tipo AND n.id IN :nivelIds "
            + "AND (:grado IS NULL OR n.grado = :grado) "
            + "AND (:nivelId IS NULL OR n.id = :nivelId) "
            + "ORDER BY i.fecha DESC, i.id DESC")
    List<IncidenteConducta> findDelPeriodoEnNiveles(@Param("direccionId") Long direccionId,
            @Param("periodoId") Long periodoId, @Param("tipo") TipoIncidente tipo,
            @Param("nivelIds") Collection<Long> nivelIds, @Param("grado") Integer grado,
            @Param("nivelId") Long nivelId);

    @Query("SELECT i FROM IncidenteConducta i JOIN FETCH i.estudiante e LEFT JOIN FETCH e.nivelAcademico "
            + "WHERE i.id = :id AND i.direccion.id = :direccionId")
    Optional<IncidenteConducta> findByIdAndDireccionId(@Param("id") Long id,
            @Param("direccionId") Long direccionId);
}
