package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaPromedio;
import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ResultadoComponente;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ResultadoComponenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class PromedioService {

    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final ResultadoComponenteRepository resultadoRepository;
    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final DistribucionPorcentualService distribucionService;
    private final ComponenteService componenteService;

    public PromedioService(UsuarioRepository usuarioRepository, NivelAcademicoRepository nivelRepository,
            MateriaRepository materiaRepository, PeriodoAcademicoRepository periodoRepository,
            ResultadoComponenteRepository resultadoRepository, AsistenciaEstudianteRepository asistenciaRepository,
            DistribucionPorcentualService distribucionService, ComponenteService componenteService) {
        this.usuarioRepository = usuarioRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.periodoRepository = periodoRepository;
        this.resultadoRepository = resultadoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.distribucionService = distribucionService;
        this.componenteService = componenteService;
    }

    public List<NivelAcademico> listarNivelesActivos(Long institucionId) {
        return nivelRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    /** El período activo más reciente de la institución (o null si no hay ninguno). */
    public PeriodoAcademico periodoActual(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId)
                .stream().findFirst().orElse(null);
    }

    public List<FilaPromedio> calcularPromedio(Long institucionId, Long nivelId, Long materiaId) {
        PeriodoAcademico periodo = periodoActual(institucionId);
        if (periodo == null) {
            return List.of();
        }
        Long periodoId = periodo.getId();

        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);

        Map<Long, List<ResultadoComponente>> resultadosPorEstudiante = resultadoRepository
                .findByComponente_Institucion_IdAndComponente_Nivel_IdAndComponente_Materia_IdAndPeriodo_Id(
                        institucionId, nivelId, materiaId, periodoId)
                .stream().collect(Collectors.groupingBy(r -> r.getEstudiante().getId()));

        DistribucionPorcentual distribucion = distribucionService.obtenerDistribucion(institucionId, periodoId, materiaId);
        Map<Long, Double> pesosCotidiano = componenteService.calcularPesosEfectivos(
                componenteService.listar(institucionId, ClaveComponente.COTIDIANO, nivelId, materiaId, null));
        Map<Long, Double> pesosTareas = componenteService.calcularPesosEfectivos(
                componenteService.listar(institucionId, ClaveComponente.TAREA, nivelId, materiaId, null));
        Map<Long, Double> pesosProyectos = componenteService.calcularPesosEfectivos(
                componenteService.listar(institucionId, ClaveComponente.PROYECTO, nivelId, materiaId, periodoId));
        Map<Long, Double> pesosExamenes = componenteService.calcularPesosEfectivos(
                componenteService.listar(institucionId, ClaveComponente.EXAMEN, nivelId, materiaId, periodoId));

        return estudiantes.stream()
                .map(est -> calcularFila(est, periodo, materiaId, institucionId,
                        resultadosPorEstudiante.getOrDefault(est.getId(), List.of()), distribucion, pesosCotidiano,
                        pesosTareas, pesosProyectos, pesosExamenes))
                .toList();
    }

    private FilaPromedio calcularFila(Usuario estudiante, PeriodoAcademico periodo, Long materiaId,
            Long institucionId, List<ResultadoComponente> resultados, DistribucionPorcentual distribucion,
            Map<Long, Double> pesosCotidiano, Map<Long, Double> pesosTareas, Map<Long, Double> pesosProyectos,
            Map<Long, Double> pesosExamenes) {
        Integer cotidiano = promedioDe(resultados, ClaveComponente.COTIDIANO, pesosCotidiano);
        Integer tareasScore = promedioDe(resultados, ClaveComponente.TAREA, pesosTareas);
        Integer proyectosScore = promedioDe(resultados, ClaveComponente.PROYECTO, pesosProyectos);
        Integer examenesScore = promedioDe(resultados, ClaveComponente.EXAMEN, pesosExamenes);

        Integer asistenciaScore = calcularAsistencia(institucionId, estudiante.getId(), materiaId, periodo);

        Double promedioFinal = promedioFinal(distribucion, cotidiano, tareasScore, proyectosScore,
                examenesScore, asistenciaScore);

        return new FilaPromedio(estudiante, cotidiano, tareasScore, proyectosScore, examenesScore,
                asistenciaScore, promedioFinal);
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

    private Integer promedioDe(List<ResultadoComponente> resultados, ClaveComponente clave, Map<Long, Double> pesos) {
        return promedioPonderado(resultados.stream()
                .filter(r -> r.getComponente().getClave() == clave)
                .map(r -> new double[] { r.getCalificacion(), pesos.getOrDefault(r.getComponente().getId(), 0.0) })
                .toList());
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
            Integer examenes, Integer asistencia) {
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
        if (sumaPesos <= 0) {
            return null;
        }
        return Math.round((sumaPonderada / sumaPesos) * 10) / 10.0;
    }
}
