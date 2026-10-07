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
    private final RebajaConductaService rebajaConductaService;

    public DistribucionPorcentualService(DistribucionPorcentualRepository distribucionRepository,
            PeriodoAcademicoRepository periodoRepository, MateriaRepository materiaRepository,
            RebajaConductaService rebajaConductaService) {
        this.distribucionRepository = distribucionRepository;
        this.periodoRepository = periodoRepository;
        this.materiaRepository = materiaRepository;
        this.rebajaConductaService = rebajaConductaService;
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodosActivos(Long direccionId) {
        return periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long direccionId) {
        return materiaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public DistribucionPorcentual obtenerDistribucion(Long direccionId, Long periodoId, Long materiaId) {
        return distribucionRepository.findByDireccionIdAndPeriodoIdAndMateriaId(direccionId, periodoId, materiaId)
                .orElseGet(() -> DistribucionPorcentual.predeterminada(
                        rebajaConductaService.asistenciaRebajaComponente(direccionId)));
    }

    /** Si las ausencias y tardías rebajan la conducta, la asistencia queda en 0 y no cuenta para el 100%. */
    public DistribucionPorcentual guardarDistribucion(Long direccionId, Long periodoId, Long materiaId,
            Integer cotidiano, Integer tareas, Integer proyectos, Integer examenes, Integer asistencia) {
        int pesoAsistencia = rebajaConductaService.asistenciaRebajaComponente(direccionId) ? safe(asistencia) : 0;
        int total = safe(cotidiano) + safe(tareas) + safe(proyectos) + safe(examenes) + pesoAsistencia;
        if (total != 100) {
            throw new IllegalArgumentException(
                    "La suma de los porcentajes debe ser exactamente 100% (actual: " + total + "%)");
        }
        PeriodoAcademico periodo = periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        DistribucionPorcentual distribucion = distribucionRepository
                .findByDireccionIdAndPeriodoIdAndMateriaId(direccionId, periodoId, materiaId)
                .orElseGet(DistribucionPorcentual::new);
        distribucion.setDireccion(periodo.getDireccion());
        distribucion.setPeriodo(periodo);
        distribucion.setMateria(materia);
        distribucion.setCotidiano(safe(cotidiano));
        distribucion.setTareas(safe(tareas));
        distribucion.setProyectos(safe(proyectos));
        distribucion.setExamenes(safe(examenes));
        distribucion.setAsistencia(pesoAsistencia);
        return distribucionRepository.save(distribucion);
    }

    private int safe(Integer valor) {
        return valor != null ? valor : 0;
    }
}
