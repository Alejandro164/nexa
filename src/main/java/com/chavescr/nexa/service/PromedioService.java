package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaPromedio;
import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.EvaluacionCotidiana;
import com.chavescr.nexa.entity.Examen;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.NotaExamen;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ProyectoEstudiantil;
import com.chavescr.nexa.entity.TareaCalificacion;
import com.chavescr.nexa.entity.TrabajoExtraclase;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.EvaluacionCotidianaRepository;
import com.chavescr.nexa.repository.ExamenRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.NotaExamenRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ProyectoEstudiantilRepository;
import com.chavescr.nexa.repository.TareaCalificacionRepository;
import com.chavescr.nexa.repository.TrabajoExtraclaseRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class PromedioService {

    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final TareaCalificacionRepository tareaRepository;
    private final ProyectoEstudiantilRepository proyectoRepository;
    private final ExamenRepository examenRepository;
    private final NotaExamenRepository notaExamenRepository;
    private final TrabajoExtraclaseRepository trabajoRepository;
    private final EvaluacionCotidianaRepository evaluacionRepository;
    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final DistribucionPorcentualService distribucionService;

    public PromedioService(UsuarioRepository usuarioRepository, NivelAcademicoRepository nivelRepository,
            MateriaRepository materiaRepository, PeriodoAcademicoRepository periodoRepository,
            TareaCalificacionRepository tareaRepository, ProyectoEstudiantilRepository proyectoRepository,
            ExamenRepository examenRepository, NotaExamenRepository notaExamenRepository,
            TrabajoExtraclaseRepository trabajoRepository, EvaluacionCotidianaRepository evaluacionRepository,
            AsistenciaEstudianteRepository asistenciaRepository, DistribucionPorcentualService distribucionService) {
        this.usuarioRepository = usuarioRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.periodoRepository = periodoRepository;
        this.tareaRepository = tareaRepository;
        this.proyectoRepository = proyectoRepository;
        this.examenRepository = examenRepository;
        this.notaExamenRepository = notaExamenRepository;
        this.trabajoRepository = trabajoRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.distribucionService = distribucionService;
    }

    public List<NivelAcademico> listarNivelesActivos(Long institucionId) {
        return nivelRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    public List<FilaPromedio> calcularPromedio(Long institucionId, Long nivelId, Long materiaId) {
        List<PeriodoAcademico> periodosActivos = periodoRepository
                .findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId);
        if (periodosActivos.isEmpty()) {
            return List.of();
        }
        PeriodoAcademico periodo = periodosActivos.get(0);
        Long periodoId = periodo.getId();

        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);

        Map<Long, List<TareaCalificacion>> tareasPorEstudiante = tareaRepository
                .findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoId(institucionId, nivelId, materiaId, periodoId)
                .stream().collect(Collectors.groupingBy(t -> t.getEstudiante().getId()));

        Map<Long, List<ProyectoEstudiantil>> proyectosPorEstudiante = proyectoRepository
                .findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaInicioAsc(institucionId, periodoId, materiaId)
                .stream().collect(Collectors.groupingBy(p -> p.getEstudiante().getId()));

        Map<Long, Examen> examenesPorId = examenRepository
                .findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaAsc(institucionId, periodoId, materiaId)
                .stream().collect(Collectors.toMap(Examen::getId, e -> e));
        Map<Long, List<NotaExamen>> notasPorEstudiante = notaExamenRepository
                .findByExamen_PeriodoIdAndExamen_MateriaId(periodoId, materiaId)
                .stream().collect(Collectors.groupingBy(n -> n.getEstudiante().getId()));

        Map<Long, List<TrabajoExtraclase>> extraclasePorEstudiante = trabajoRepository
                .findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaEntregaAsc(institucionId, periodoId, materiaId)
                .stream().collect(Collectors.groupingBy(t -> t.getEstudiante().getId()));

        Map<Long, List<EvaluacionCotidiana>> evaluacionesPorEstudiante = evaluacionRepository
                .findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoId(institucionId, nivelId, materiaId, periodoId)
                .stream().collect(Collectors.groupingBy(ev -> ev.getEstudiante().getId()));

        DistribucionPorcentual distribucion = distribucionService.obtenerDistribucion(institucionId, periodoId, materiaId);

        return estudiantes.stream()
                .map(est -> calcularFila(est, periodo, materiaId, institucionId,
                        tareasPorEstudiante.getOrDefault(est.getId(), List.of()),
                        proyectosPorEstudiante.getOrDefault(est.getId(), List.of()),
                        notasPorEstudiante.getOrDefault(est.getId(), List.of()), examenesPorId,
                        extraclasePorEstudiante.getOrDefault(est.getId(), List.of()),
                        evaluacionesPorEstudiante.getOrDefault(est.getId(), List.of()), distribucion))
                .toList();
    }

    private FilaPromedio calcularFila(Usuario estudiante, PeriodoAcademico periodo, Long materiaId,
            Long institucionId, List<TareaCalificacion> tareas, List<ProyectoEstudiantil> proyectos,
            List<NotaExamen> notas, Map<Long, Examen> examenesPorId, List<TrabajoExtraclase> extraclase,
            List<EvaluacionCotidiana> evaluaciones, DistribucionPorcentual distribucion) {
        Integer cotidiano = promedioPonderado(evaluaciones.stream()
                .map(ev -> new double[] { ev.getCalificacion(), ev.getIndicador().getPorcentaje() })
                .toList());

        Integer tareasScore = promedioPonderado(tareas.stream()
                .map(t -> new double[] { t.getCalificacion(), t.getTareaDefinicion().getPorcentaje() })
                .toList());

        Integer proyectosScore = promedioPonderado(proyectos.stream()
                .filter(p -> p.getCalificacion() != null)
                .map(p -> new double[] { p.getCalificacion(), p.getPorcentaje() })
                .toList());

        Integer examenesScore = promedioPonderado(notas.stream()
                .filter(n -> examenesPorId.containsKey(n.getExamen().getId()))
                .map(n -> new double[] { n.getCalificacion(), examenesPorId.get(n.getExamen().getId()).getPorcentaje() })
                .toList());

        Integer extraclaseScore = promedioPonderado(extraclase.stream()
                .filter(t -> t.getCalificacion() != null)
                .map(t -> new double[] { t.getCalificacion(), t.getPorcentaje() })
                .toList());

        Integer asistenciaScore = calcularAsistencia(institucionId, estudiante.getId(), materiaId, periodo);

        Double promedioFinal = promedioFinal(distribucion, cotidiano, tareasScore, proyectosScore,
                examenesScore, asistenciaScore, extraclaseScore);

        return new FilaPromedio(estudiante, cotidiano, tareasScore, proyectosScore, examenesScore,
                extraclaseScore, asistenciaScore, promedioFinal);
    }

    private Integer calcularAsistencia(Long institucionId, Long estudianteId, Long materiaId,
            PeriodoAcademico periodo) {
        List<AsistenciaEstudiante> registros = asistenciaRepository
                .findByInstitucionIdAndEstudianteIdAndMateriaIdAndFechaBetween(institucionId, estudianteId,
                        materiaId, periodo.getFechaInicio(), periodo.getFechaFin());
        if (registros.isEmpty()) {
            return null;
        }
        long presentes = registros.stream()
                .filter(r -> r.getEstado() == AsistenciaEstudiante.EstadoAsistencia.PRESENTE
                        || r.getEstado() == AsistenciaEstudiante.EstadoAsistencia.TARDIA)
                .count();
        return (int) Math.round(presentes * 100.0 / registros.size());
    }

    /** Promedio ponderado (0-100) de una lista de [calificacion, peso]; null si la lista está vacía. */
    private Integer promedioPonderado(List<double[]> pares) {
        double sumaPesos = pares.stream().mapToDouble(p -> p[1]).sum();
        if (pares.isEmpty() || sumaPesos <= 0) {
            return null;
        }
        double suma = pares.stream().mapToDouble(p -> p[0] * p[1]).sum();
        return (int) Math.round(suma / sumaPesos);
    }

    private Double promedioFinal(DistribucionPorcentual d, Integer cotidiano, Integer tareas, Integer proyectos,
            Integer examenes, Integer asistencia, Integer extraclase) {
        double sumaPonderada = 0;
        double sumaPesos = 0;
        if (cotidiano != null) {
            sumaPonderada += cotidiano * d.getCotidiano();
            sumaPesos += d.getCotidiano();
        }
        if (tareas != null) {
            sumaPonderada += tareas * d.getTareas();
            sumaPesos += d.getTareas();
        }
        if (proyectos != null) {
            sumaPonderada += proyectos * d.getProyectos();
            sumaPesos += d.getProyectos();
        }
        if (examenes != null) {
            sumaPonderada += examenes * d.getExamenes();
            sumaPesos += d.getExamenes();
        }
        if (asistencia != null) {
            sumaPonderada += asistencia * d.getAsistencia();
            sumaPesos += d.getAsistencia();
        }
        if (extraclase != null) {
            sumaPonderada += extraclase * d.getTrabajosExtraclase();
            sumaPesos += d.getTrabajosExtraclase();
        }
        if (sumaPesos <= 0) {
            return null;
        }
        return Math.round((sumaPonderada / sumaPesos) * 10) / 10.0;
    }
}
