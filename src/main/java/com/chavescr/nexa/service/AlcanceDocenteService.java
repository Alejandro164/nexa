package com.chavescr.nexa.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DocenteMateria;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.repository.DocenteMateriaRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;

/**
 * Alcance de los filtros de asistencia.
 * Director y administrador ({@code docenteId} null) ven el catálogo completo de materias
 * activas y todas las secciones de todos los grados. El docente solo ve las materias que
 * imparte en el horario o tiene asociadas, las secciones en las que las da y las lecciones
 * que tiene a cargo.
 */
@Service
@Transactional(readOnly = true)
public class AlcanceDocenteService {

    private final HorarioLeccionRepository horarioLeccionRepository;
    private final MateriaRepository materiaRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final DocenteMateriaRepository docenteMateriaRepository;

    public AlcanceDocenteService(HorarioLeccionRepository horarioLeccionRepository,
            MateriaRepository materiaRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            DocenteMateriaRepository docenteMateriaRepository) {
        this.horarioLeccionRepository = horarioLeccionRepository;
        this.materiaRepository = materiaRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.docenteMateriaRepository = docenteMateriaRepository;
    }

    public List<Materia> materiasVisibles(Long direccionId, Long docenteId) {
        if (docenteId == null) {
            return materiaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
        }
        return horarioLeccionRepository.findMateriasDistinctByDireccionIdAndDocenteId(direccionId, docenteId);
    }

    public List<NivelAcademico> nivelesVisibles(Long direccionId, Long docenteId) {
        if (docenteId == null) {
            return nivelAcademicoRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
        }
        return horarioLeccionRepository.findNivelesDistinctByDireccionIdAndDocenteId(direccionId, docenteId);
    }

    public List<Materia> materiasVisiblesEnPeriodo(Long direccionId, Long periodoId, Long docenteId) {
        if (docenteId == null) {
            return materiaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
        }
        if (periodoId == null) {
            return List.of();
        }
        Map<Long, Materia> porId = new LinkedHashMap<>();
        for (Materia materia : horarioLeccionRepository.findMateriasDistinctByDireccionIdAndPeriodoIdAndDocenteId(
                direccionId, periodoId, docenteId)) {
            if (Boolean.TRUE.equals(materia.getActivo())) {
                porId.put(materia.getId(), materia);
            }
        }
        for (DocenteMateria asignacion : docenteMateriaRepository
                .findByDireccionIdAndDocenteIdOrderByMateria_NombreAsc(direccionId, docenteId)) {
            Materia materia = asignacion.getMateria();
            if (materia != null && Boolean.TRUE.equals(materia.getActivo())) {
                porId.putIfAbsent(materia.getId(), materia);
            }
        }
        return porId.values().stream()
                .sorted(Comparator.comparing(Materia::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<NivelAcademico> nivelesVisiblesEnPeriodoPorMateria(Long direccionId, Long periodoId, Long materiaId,
            Long docenteId) {
        if (docenteId == null) {
            return nivelAcademicoRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
        }
        if (periodoId == null || materiaId == null) {
            return List.of();
        }
        return horarioLeccionRepository.findNivelesDistinctByDireccionIdAndPeriodoIdAndMateriaIdAndDocenteId(
                direccionId, periodoId, materiaId, docenteId);
    }

    /**
     * Lecciones de esa materia/sección en el período. Si el día tiene horario, se usan esas;
     * si no, se listan las del período para que el select no quede vacío.
     */
    public List<Integer> leccionesVisiblesEnPeriodo(Long direccionId, Long periodoId, Long nivelId, Long materiaId,
            String dia, Long docenteId) {
        if (periodoId == null || nivelId == null || materiaId == null) {
            return List.of();
        }
        List<Integer> delDia = numerosLeccionEnPeriodo(direccionId, periodoId, nivelId, materiaId, dia, docenteId);
        if (!delDia.isEmpty() || dia == null) {
            return delDia;
        }
        return numerosLeccionEnPeriodo(direccionId, periodoId, nivelId, materiaId, null, docenteId);
    }

    private List<Integer> numerosLeccionEnPeriodo(Long direccionId, Long periodoId, Long nivelId, Long materiaId,
            String dia, Long docenteId) {
        if (docenteId == null) {
            return horarioLeccionRepository.findNumerosLeccionByDireccionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDia(
                    direccionId, periodoId, nivelId, materiaId, dia);
        }
        return horarioLeccionRepository
                .findNumerosLeccionByDireccionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDiaAndDocenteId(
                        direccionId, periodoId, nivelId, materiaId, dia, docenteId);
    }
}
