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

    public List<Materia> materiasVisibles(Long institucionId, Long docenteId) {
        if (docenteId == null) {
            return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
        }
        return horarioLeccionRepository.findMateriasDistinctByInstitucionIdAndDocenteId(institucionId, docenteId);
    }

    public List<NivelAcademico> nivelesVisibles(Long institucionId, Long docenteId) {
        if (docenteId == null) {
            return nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
        }
        return horarioLeccionRepository.findNivelesDistinctByInstitucionIdAndDocenteId(institucionId, docenteId);
    }

    public List<Materia> materiasVisiblesEnPeriodo(Long institucionId, Long periodoId, Long docenteId) {
        if (periodoId == null) {
            return List.of();
        }
        if (docenteId == null) {
            return horarioLeccionRepository.findMateriasDistinctByInstitucionIdAndPeriodoId(institucionId, periodoId);
        }
        return horarioLeccionRepository.findMateriasDistinctByInstitucionIdAndPeriodoIdAndDocenteId(
                institucionId, periodoId, docenteId);
    }

    public List<NivelAcademico> nivelesVisiblesEnPeriodoPorMateria(Long institucionId, Long periodoId, Long materiaId,
            Long docenteId) {
        if (periodoId == null || materiaId == null) {
            return List.of();
        }
        if (docenteId == null) {
            return horarioLeccionRepository.findNivelesDistinctByInstitucionIdAndPeriodoIdAndMateriaId(
                    institucionId, periodoId, materiaId);
        }
        return horarioLeccionRepository.findNivelesDistinctByInstitucionIdAndPeriodoIdAndMateriaIdAndDocenteId(
                institucionId, periodoId, materiaId, docenteId);
    }

    /**
     * Lecciones de esa materia/sección en el período. Si el día tiene horario, se usan esas;
     * si no, se listan las del período para que el select no quede vacío.
     */
    public List<Integer> leccionesVisiblesEnPeriodo(Long institucionId, Long periodoId, Long nivelId, Long materiaId,
            String dia, Long docenteId) {
        if (periodoId == null || nivelId == null || materiaId == null) {
            return List.of();
        }
        List<Integer> delDia = numerosLeccionEnPeriodo(institucionId, periodoId, nivelId, materiaId, dia, docenteId);
        if (!delDia.isEmpty() || dia == null) {
            return delDia;
        }
        return numerosLeccionEnPeriodo(institucionId, periodoId, nivelId, materiaId, null, docenteId);
    }

    private List<Integer> numerosLeccionEnPeriodo(Long institucionId, Long periodoId, Long nivelId, Long materiaId,
            String dia, Long docenteId) {
        if (docenteId == null) {
            return horarioLeccionRepository.findNumerosLeccionByInstitucionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDia(
                    institucionId, periodoId, nivelId, materiaId, dia);
        }
        return horarioLeccionRepository
                .findNumerosLeccionByInstitucionIdAndPeriodoIdAndNivelIdAndMateriaIdAndDiaAndDocenteId(
                        institucionId, periodoId, nivelId, materiaId, dia, docenteId);
    }
}
