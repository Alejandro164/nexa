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
    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndNivelIdOrderByNumeroLeccionAsc(
            Long institucionId, Long periodoId, Long nivelId);

    boolean existsByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);

    @Query("SELECT DISTINCT h.materia FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.docente.id = :docenteId " +
            "ORDER BY h.materia.nombre")
    List<Materia> findMateriasDistinctByInstitucionIdAndDocenteId(
            @Param("institucionId") Long institucionId, @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.nivel FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.docente.id = :docenteId " +
            "ORDER BY h.nivel.grado, h.nivel.seccion")
    List<NivelAcademico> findNivelesDistinctByInstitucionIdAndDocenteId(
            @Param("institucionId") Long institucionId, @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.materia FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId " +
            "ORDER BY h.materia.nombre")
    List<Materia> findMateriasDistinctByInstitucionIdAndPeriodoId(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId);

    @Query("SELECT DISTINCT h.materia FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId " +
            "AND h.docente.id = :docenteId ORDER BY h.materia.nombre")
    List<Materia> findMateriasDistinctByInstitucionIdAndPeriodoIdAndDocenteId(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.nivel FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId " +
            "AND h.materia.id = :materiaId ORDER BY h.nivel.grado, h.nivel.seccion")
    List<NivelAcademico> findNivelesDistinctByInstitucionIdAndPeriodoIdAndMateriaId(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("materiaId") Long materiaId);

    @Query("SELECT DISTINCT h.nivel FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId " +
            "AND h.materia.id = :materiaId AND h.docente.id = :docenteId " +
            "ORDER BY h.nivel.grado, h.nivel.seccion")
    List<NivelAcademico> findNivelesDistinctByInstitucionIdAndPeriodoIdAndMateriaIdAndDocenteId(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("materiaId") Long materiaId, @Param("docenteId") Long docenteId);

    @Query("SELECT DISTINCT h.numeroLeccion FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId AND h.nivel.id = :nivelId " +
            "AND h.materia.id = :materiaId AND (:dia IS NULL OR h.dia = :dia) ORDER BY h.numeroLeccion")
    List<Integer> findNumerosLeccionByInstitucionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDia(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("nivelId") Long nivelId, @Param("materiaId") Long materiaId, @Param("dia") String dia);

    @Query("SELECT DISTINCT h.numeroLeccion FROM HorarioLeccion h " +
            "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId AND h.nivel.id = :nivelId " +
            "AND h.materia.id = :materiaId AND (:dia IS NULL OR h.dia = :dia) AND h.docente.id = :docenteId " +
            "ORDER BY h.numeroLeccion")
    List<Integer> findNumerosLeccionByInstitucionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDiaAndDocenteId(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("nivelId") Long nivelId, @Param("materiaId") Long materiaId,
            @Param("dia") String dia, @Param("docenteId") Long docenteId);

    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccionOrderByIdAsc(
            Long institucionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long institucionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    @Query("SELECT DISTINCT h.docente.id FROM HorarioLeccion h "
            + "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId "
            + "AND h.dia = :dia AND h.numeroLeccion = :numeroLeccion "
            + "AND (:leccionId IS NULL OR h.id <> :leccionId)")
    List<Long> findDocenteIdsEnBloque(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("dia") String dia, @Param("numeroLeccion") Integer numeroLeccion,
            @Param("leccionId") Long leccionId);

    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndDocenteIdOrderByDiaAscNumeroLeccionAsc(
            Long institucionId, Long periodoId, Long docenteId);

    @Query("SELECT h.docente.id, COUNT(h) FROM HorarioLeccion h "
            + "WHERE h.institucion.id = :institucionId AND h.periodo.id = :periodoId "
            + "GROUP BY h.docente.id")
    List<Object[]> countLeccionesGroupedByDocente(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId);

    Optional<HorarioLeccion> findByIdAndInstitucionId(Long id, Long institucionId);

    void deleteByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);
    void deleteByInstitucionIdAndNivelId(Long institucionId, Long nivelId);
    void deleteByInstitucionIdAndMateriaId(Long institucionId, Long materiaId);
    void deleteByInstitucionIdAndAulaId(Long institucionId, Long aulaId);
}
