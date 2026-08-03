package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaEvaluacionCotidiana;
import com.chavescr.nexa.entity.EvaluacionCotidiana;
import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.EvaluacionCotidianaRepository;
import com.chavescr.nexa.repository.IndicadorCotidianoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class EvaluacionCotidianaService {

    private final EvaluacionCotidianaRepository evaluacionRepository;
    private final IndicadorCotidianoRepository indicadorRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;

    public EvaluacionCotidianaService(EvaluacionCotidianaRepository evaluacionRepository,
            IndicadorCotidianoRepository indicadorRepository, PeriodoAcademicoRepository periodoRepository,
            UsuarioRepository usuarioRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.indicadorRepository = indicadorRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Indicadores definidos para esta sección y materia. */
    @Transactional(readOnly = true)
    public List<IndicadorCotidiano> listarIndicadoresDisponibles(Long institucionId, Long nivelId, Long materiaId) {
        return indicadorRepository.findByInstitucionIdAndNivelIdAndMateriaIdOrderByIdAsc(
                institucionId, nivelId, materiaId);
    }

    @Transactional(readOnly = true)
    public IndicadorCotidiano obtenerIndicador(Long institucionId, Long id) {
        return indicadorRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Indicador no encontrado"));
    }

    /** El período activo más reciente de la institución; la evaluación de cotidiano se lleva a ese nivel, no diario. */
    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivo(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay un período académico activo"));
    }

    /** Una fila por cada estudiante activo de la sección, con su calificación de ese indicador en el período activo. */
    @Transactional(readOnly = true)
    public List<FilaEvaluacionCotidiana> listarFilas(Long institucionId, Long nivelId, Long indicadorId,
            Long periodoId) {
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
        Map<Long, EvaluacionCotidiana> registros = evaluacionRepository
                .findByInstitucionIdAndIndicadorIdAndPeriodoId(institucionId, indicadorId, periodoId).stream()
                .collect(Collectors.toMap(e -> e.getEstudiante().getId(), e -> e));
        return estudiantes.stream()
                .map(e -> construirFila(e, registros.get(e.getId())))
                .toList();
    }

    public FilaEvaluacionCotidiana registrarCalificacion(Long institucionId, Long estudianteId, Long indicadorId,
            Long periodoId, Integer calificacion, String observacion) {
        IndicadorCotidiano indicador = indicadorRepository.findByIdAndInstitucionId(indicadorId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Indicador no encontrado"));
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        EvaluacionCotidiana evaluacion = evaluacionRepository
                .findByInstitucionIdAndEstudianteIdAndIndicadorIdAndPeriodoId(institucionId, estudianteId, indicadorId, periodoId)
                .orElseGet(EvaluacionCotidiana::new);

        // Cada input (calificación / observación) guarda por separado; se conserva el valor existente
        // del otro campo cuando no viene en esta petición, igual que en Asistencia.
        Integer calificacionFinal = calificacion != null ? calificacion : evaluacion.getCalificacion();
        if (calificacionFinal == null) {
            throw new IllegalArgumentException("Debes asignar primero una calificación");
        }
        if (calificacionFinal < 0 || calificacionFinal > 100) {
            throw new IllegalArgumentException("La calificación debe estar entre 0 y 100");
        }
        String observacionFinal = evaluacion.getObservacion();
        if (observacion != null) {
            observacionFinal = observacion.isBlank() ? null : observacion.trim();
        }

        evaluacion.setInstitucion(indicador.getInstitucion());
        evaluacion.setIndicador(indicador);
        evaluacion.setEstudiante(estudiante);
        evaluacion.setPeriodo(periodo);
        evaluacion.setCalificacion(calificacionFinal);
        evaluacion.setObservacion(observacionFinal);
        evaluacionRepository.save(evaluacion);

        return construirFila(estudiante, evaluacion);
    }

    private FilaEvaluacionCotidiana construirFila(Usuario estudiante, EvaluacionCotidiana registro) {
        return new FilaEvaluacionCotidiana(estudiante,
                registro != null ? registro.getCalificacion() : null,
                registro != null ? registro.getObservacion() : null);
    }
}
