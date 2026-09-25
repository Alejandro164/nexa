package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;

public interface HorarioLeccionRepository extends JpaRepository<HorarioLeccion, Long> {
    List<HorarioLeccion> findByDireccionIdAndPeriodoIdAndNivelIdOrderByNumeroLeccionAsc(
            Long direccionId, Long periodoId, Long nivelId);

    boolean existsByDireccionIdAndPeriodoId(Long direccionId, Long periodoId);

    @Query("SELECT DISTINCT h.materia FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.docente.id = :docenteId " +
            "ORDER BY h.materia.nombre")
    List<Materia> findMateriasDistinctByDireccionIdAndDocenteId(
            @Param("direccionId") Long direccionId, @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.nivel FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.docente.id = :docenteId " +
            "ORDER BY h.nivel.grado, h.nivel.seccion")
    List<NivelAcademico> findNivelesDistinctByDireccionIdAndDocenteId(
            @Param("direccionId") Long direccionId, @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.materia FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId " +
            "ORDER BY h.materia.nombre")
    List<Materia> findMateriasDistinctByDireccionIdAndPeriodoId(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId);

    @Query("SELECT DISTINCT h.materia FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId " +
            "AND h.docente.id = :docenteId ORDER BY h.materia.nombre")
    List<Materia> findMateriasDistinctByDireccionIdAndPeriodoIdAndDocenteId(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.nivel FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId " +
            "AND h.materia.id = :materiaId ORDER BY h.nivel.grado, h.nivel.seccion")
    List<NivelAcademico> findNivelesDistinctByDireccionIdAndPeriodoIdAndMateriaId(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("materiaId") Long materiaId);

    @Query("SELECT DISTINCT h.nivel FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId " +
            "AND h.materia.id = :materiaId AND h.docente.id = :docenteId " +
            "ORDER BY h.nivel.grado, h.nivel.seccion")
    List<NivelAcademico> findNivelesDistinctByDireccionIdAndPeriodoIdAndMateriaIdAndDocenteId(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("materiaId") Long materiaId, @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.numeroLeccion FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId AND h.nivel.id = :nivelId " +
            "AND h.materia.id = :materiaId AND (:dia IS NULL OR h.dia = :dia) ORDER BY h.numeroLeccion")
    List<Integer> findNumerosLeccionByDireccionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDia(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("nivelId") Long nivelId, @Param("materiaId") Long materiaId, @Param("dia") String dia);

    @Query("SELECT DISTINCT h.numeroLeccion FROM HorarioLeccion h " +
            "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId AND h.nivel.id = :nivelId " +
            "AND h.materia.id = :materiaId AND (:dia IS NULL OR h.dia = :dia) AND h.docente.id = :docenteId " +
            "ORDER BY h.numeroLeccion")
    List<Integer> findNumerosLeccionByDireccionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDiaAndDocenteId(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("nivelId") Long nivelId, @Param("materiaId") Long materiaId,
            @Param("dia") String dia, @Param("docenteId") Long docenteId);

    List<HorarioLeccion> findByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccionOrderByIdAsc(
            Long direccionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    List<HorarioLeccion> findByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long direccionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    @Query("SELECT DISTINCT h.docente.id FROM HorarioLeccion h "
            + "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId "
            + "AND h.dia = :dia AND h.numeroLeccion = :numeroLeccion "
            + "AND (:leccionId IS NULL OR h.id <> :leccionId)")
    List<Long> findDocenteIdsEnBloque(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("dia") String dia, @Param("numeroLeccion") Integer numeroLeccion,
            @Param("leccionId") Long leccionId);

    List<HorarioLeccion> findByDireccionIdAndPeriodoIdAndDocenteIdOrderByDiaAscNumeroLeccionAsc(
            Long direccionId, Long periodoId, Long docenteId);

    @Query("SELECT h.docente.id, COUNT(h) FROM HorarioLeccion h "
            + "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId "
            + "GROUP BY h.docente.id")
    List<Object[]> countLeccionesGroupedByDocente(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId);

    Optional<HorarioLeccion> findByIdAndDireccionId(Long id, Long direccionId);

    List<HorarioLeccion> findByDireccionId(Long direccionId);

    boolean existsByDireccionIdAndNumeroLeccionGreaterThan(Long direccionId, Integer numeroLeccion);

    boolean existsByDireccionIdAndDia(Long direccionId, String dia);

    boolean existsByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
            Long direccionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    void deleteByDireccionIdAndPeriodoId(Long direccionId, Long periodoId);
    void deleteByDireccionIdAndNivelId(Long direccionId, Long nivelId);
    void deleteByDireccionIdAndMateriaId(Long direccionId, Long materiaId);
    void deleteByDireccionIdAndAulaId(Long direccionId, Long aulaId);

    @Query("SELECT DISTINCT h.docente.id, h.materia.id, h.nivel.id FROM HorarioLeccion h "
            + "WHERE h.direccion.id = :direccionId AND h.periodo.id = :periodoId")
    List<Object[]> findCombosDocenteMateriaNivel(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId);
}
