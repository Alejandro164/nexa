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
}
