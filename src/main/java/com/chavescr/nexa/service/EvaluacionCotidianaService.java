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

    /** Igual que {@link #obtenerPeriodoActivo}, pero null en vez de lanzar si no hay ninguno activo. */
    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivoOpcional(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId).stream()
                .findFirst()
                .orElse(null);
    }

    /** Cuántos estudiantes activos tiene la sección; para mostrar "evaluados / total" junto a los indicadores. */
    @Transactional(readOnly = true)
    public int contarEstudiantesActivos(Long nivelId) {
        return usuarioRepository.findEstudiantesActivosByNivelId(nivelId).size();
    }

    /** Cuántos estudiantes distintos ya tienen una calificación registrada, por indicador, en ese período. */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarEvaluadosPorIndicador(Long institucionId, List<Long> indicadorIds, Long periodoId) {
        if (indicadorIds == null || indicadorIds.isEmpty() || periodoId == null) {
            return Map.of();
        }
        return evaluacionRepository.findByInstitucionIdAndIndicadorIdInAndPeriodoId(institucionId, indicadorIds, periodoId)
                .stream()
                .collect(Collectors.groupingBy(e -> e.getIndicador().getId(), Collectors.counting()));
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

    /**
     * Si el indicador tiene puntosTotales definidos, la calificación se calcula a partir de
     * puntosObtenidos; si no, se ingresa directamente (0-100). Los puntos totales del indicador son
     * opcionales, así que este método soporta ambos modos según ese indicador en particular.
     */
    public FilaEvaluacionCotidiana registrarCalificacion(Long institucionId, Long estudianteId, Long indicadorId,
            Long periodoId, Integer calificacion, Integer puntosObtenidos, String observacion) {
        IndicadorCotidiano indicador = indicadorRepository.findByIdAndInstitucionId(indicadorId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Indicador no encontrado"));
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        EvaluacionCotidiana evaluacion = evaluacionRepository
                .findByInstitucionIdAndEstudianteIdAndIndicadorIdAndPeriodoId(institucionId, estudianteId, indicadorId, periodoId)
                .orElseGet(EvaluacionCotidiana::new);

        Integer calificacionFinal;
        Integer puntosFinal = null;
        if (indicador.getPuntosTotales() != null) {
            puntosFinal = puntosObtenidos != null ? puntosObtenidos : evaluacion.getPuntosObtenidos();
            if (puntosFinal == null) {
                throw new IllegalArgumentException("Debes asignar primero los puntos obtenidos");
            }
            if (puntosFinal < 0 || puntosFinal > indicador.getPuntosTotales()) {
                throw new IllegalArgumentException(
                        "Los puntos obtenidos deben estar entre 0 y " + indicador.getPuntosTotales());
            }
            calificacionFinal = (int) Math.round(puntosFinal * 100.0 / indicador.getPuntosTotales());
        } else {
            // Cada input (calificación / observación) guarda por separado; se conserva el valor
            // existente del otro campo cuando no viene en esta petición, igual que en Asistencia.
            calificacionFinal = calificacion != null ? calificacion : evaluacion.getCalificacion();
            if (calificacionFinal == null) {
                throw new IllegalArgumentException("Debes asignar primero una calificación");
            }
            if (calificacionFinal < 0 || calificacionFinal > 100) {
                throw new IllegalArgumentException("La calificación debe estar entre 0 y 100");
            }
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
        evaluacion.setPuntosObtenidos(puntosFinal);
        evaluacion.setObservacion(observacionFinal);
        evaluacionRepository.save(evaluacion);

        return construirFila(estudiante, evaluacion);
    }

    private FilaEvaluacionCotidiana construirFila(Usuario estudiante, EvaluacionCotidiana registro) {
        return new FilaEvaluacionCotidiana(estudiante,
                registro != null ? registro.getPuntosObtenidos() : null,
                registro != null ? registro.getCalificacion() : null,
                registro != null ? registro.getObservacion() : null);
    }
}
