package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.repository.DistribucionPorcentualRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;

@Service
@Transactional
public class DistribucionPorcentualService {

    private final DistribucionPorcentualRepository distribucionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MateriaRepository materiaRepository;

    public DistribucionPorcentualService(DistribucionPorcentualRepository distribucionRepository,
            PeriodoAcademicoRepository periodoRepository, MateriaRepository materiaRepository) {
        this.distribucionRepository = distribucionRepository;
        this.periodoRepository = periodoRepository;
        this.materiaRepository = materiaRepository;
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodosActivos(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public DistribucionPorcentual obtenerDistribucion(Long institucionId, Long periodoId, Long materiaId) {
        return distribucionRepository.findByInstitucionIdAndPeriodoIdAndMateriaId(institucionId, periodoId, materiaId)
                .orElseGet(() -> {
                    DistribucionPorcentual nueva = new DistribucionPorcentual();
                    nueva.setCotidiano(40);
                    nueva.setTareas(15);
                    nueva.setProyectos(20);
                    nueva.setExamenes(20);
                    nueva.setAsistencia(5);
                    return nueva;
                });
    }

    public DistribucionPorcentual guardarDistribucion(Long institucionId, Long periodoId, Long materiaId,
            Integer cotidiano, Integer tareas, Integer proyectos, Integer examenes, Integer asistencia) {
        int total = safe(cotidiano) + safe(tareas) + safe(proyectos) + safe(examenes) + safe(asistencia);
        if (total != 100) {
            throw new IllegalArgumentException(
                    "La suma de los porcentajes debe ser exactamente 100% (actual: " + total + "%)");
        }
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        DistribucionPorcentual distribucion = distribucionRepository
                .findByInstitucionIdAndPeriodoIdAndMateriaId(institucionId, periodoId, materiaId)
                .orElseGet(DistribucionPorcentual::new);
        distribucion.setInstitucion(periodo.getInstitucion());
        distribucion.setPeriodo(periodo);
        distribucion.setMateria(materia);
        distribucion.setCotidiano(safe(cotidiano));
        distribucion.setTareas(safe(tareas));
        distribucion.setProyectos(safe(proyectos));
        distribucion.setExamenes(safe(examenes));
        distribucion.setAsistencia(safe(asistencia));
        return distribucionRepository.save(distribucion);
    }

    private int safe(Integer valor) {
        return valor != null ? valor : 0;
    }
}
