package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;

/**
 * Filtra las materias y secciones visibles en el módulo de gestión académica según
 * lo que el docente tiene efectivamente asignado en el horario. Con docenteId null
 * (director/admin) no se restringe nada.
 */
@Service
@Transactional(readOnly = true)
public class AlcanceDocenteService {

    private final HorarioLeccionRepository horarioLeccionRepository;
    private final MateriaRepository materiaRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;

    public AlcanceDocenteService(HorarioLeccionRepository horarioLeccionRepository,
            MateriaRepository materiaRepository,
            NivelAcademicoRepository nivelAcademicoRepository) {
        this.horarioLeccionRepository = horarioLeccionRepository;
        this.materiaRepository = materiaRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
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
        if (periodoId == null) {
            return List.of();
        }
        if (docenteId == null) {
            return horarioLeccionRepository.findMateriasDistinctByDireccionIdAndPeriodoId(direccionId, periodoId);
        }
        return horarioLeccionRepository.findMateriasDistinctByDireccionIdAndPeriodoIdAndDocenteId(
                direccionId, periodoId, docenteId);
    }

    public List<NivelAcademico> nivelesVisiblesEnPeriodoPorMateria(Long direccionId, Long periodoId, Long materiaId,
            Long docenteId) {
        if (periodoId == null || materiaId == null) {
            return List.of();
        }
        if (docenteId == null) {
            return horarioLeccionRepository.findNivelesDistinctByDireccionIdAndPeriodoIdAndMateriaId(
                    direccionId, periodoId, materiaId);
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
