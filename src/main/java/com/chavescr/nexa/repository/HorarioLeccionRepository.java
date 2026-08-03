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

    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccionOrderByIdAsc(
            Long institucionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion);

    List<HorarioLeccion> findByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long institucionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    Optional<HorarioLeccion> findByIdAndInstitucionId(Long id, Long institucionId);

    void deleteByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);
    void deleteByInstitucionIdAndNivelId(Long institucionId, Long nivelId);
    void deleteByInstitucionIdAndMateriaId(Long institucionId, Long materiaId);
    void deleteByInstitucionIdAndAulaId(Long institucionId, Long aulaId);
}
