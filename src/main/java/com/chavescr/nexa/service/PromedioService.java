package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.EnumMap;
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
import com.chavescr.nexa.entity.TipoComponente;
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

    public List<NivelAcademico> listarNivelesActivos(Long direccionId) {
        return nivelRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
    }

    public List<Materia> listarMateriasActivas(Long direccionId) {
        return materiaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
    }

    /** El período activo más reciente de la dirección (o null si no hay ninguno). */
    public PeriodoAcademico periodoActual(Long direccionId) {
        return periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId)
                .stream().findFirst().orElse(null);
    }

    public List<FilaPromedio> calcularPromedio(Long direccionId, Long nivelId, Long materiaId,
            List<TipoComponente> tipos) {
        PeriodoAcademico periodo = periodoActual(direccionId);
        if (periodo == null) {
            return List.of();
        }
        Long periodoId = periodo.getId();

        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);

        Map<Long, List<ResultadoComponente>> resultadosPorEstudiante = resultadoRepository
                .findByComponente_Direccion_IdAndComponente_Nivel_IdAndComponente_Materia_IdAndPeriodo_Id(
                        direccionId, nivelId, materiaId, periodoId)
                .stream().collect(Collectors.groupingBy(r -> r.getEstudiante().getId()));

        DistribucionPorcentual distribucion = distribucionService.obtenerDistribucion(direccionId, periodoId, materiaId);
        List<TipoComponente> columnas = tipos == null ? List.of() : tipos;
        Map<ClaveComponente, Map<Long, Double>> pesos = pesosDe(direccionId, nivelId, materiaId, periodoId, columnas);

        return estudiantes.stream()
                .map(est -> calcularFila(est, periodo, materiaId, direccionId,
                        resultadosPorEstudiante.getOrDefault(est.getId(), List.of()), distribucion, columnas, pesos))
                .toList();
    }

    private Map<ClaveComponente, Map<Long, Double>> pesosDe(Long direccionId, Long nivelId, Long materiaId,
            Long periodoId, List<TipoComponente> tipos) {
        Map<ClaveComponente, Map<Long, Double>> pesos = new EnumMap<>(ClaveComponente.class);
        for (TipoComponente tipo : tipos) {
            if (tipo.getClave() == null || pesos.containsKey(tipo.getClave())) {
                continue;
            }
            Long periodoDelTipo = tipo.getClave() == ClaveComponente.PROYECTO
                    || tipo.getClave() == ClaveComponente.EXAMEN ? periodoId : null;
            pesos.put(tipo.getClave(), componenteService.calcularPesosEfectivos(
                    componenteService.listar(direccionId, tipo.getClave(), nivelId, materiaId, periodoDelTipo)));
        }
        return pesos;
    }

    private FilaPromedio calcularFila(Usuario estudiante, PeriodoAcademico periodo, Long materiaId,
            Long direccionId, List<ResultadoComponente> resultados, DistribucionPorcentual distribucion,
            List<TipoComponente> tipos, Map<ClaveComponente, Map<Long, Double>> pesos) {
        List<Integer> notas = new ArrayList<>();
        for (TipoComponente tipo : tipos) {
            if (tipo.getClave() == null) {
                notas.add(null);
                continue;
            }
            notas.add(promedioDe(resultados, tipo.getClave(), pesos.getOrDefault(tipo.getClave(), Map.of())));
        }

        Integer asistenciaScore = calcularAsistencia(direccionId, estudiante.getId(), materiaId, periodo);
        Double promedioFinal = promedioFinal(distribucion, tipos, notas, asistenciaScore);
        return new FilaPromedio(estudiante, notas, asistenciaScore, promedioFinal);
    }

    private Integer calcularAsistencia(Long direccionId, Long estudianteId, Long materiaId,
            PeriodoAcademico periodo) {
        List<AsistenciaEstudiante> registros = asistenciaRepository
                .findByDireccionIdAndEstudianteIdAndMateriaIdAndFechaBetween(direccionId, estudianteId,
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

    private Double promedioFinal(DistribucionPorcentual d, List<TipoComponente> tipos, List<Integer> notas,
            Integer asistencia) {
        double sumaPonderada = 0;
        double sumaPesos = 0;
        for (int i = 0; i < tipos.size(); i++) {
            Integer nota = notas.get(i);
            Integer peso = pesoDe(d, tipos.get(i).getClave());
            if (nota == null || peso == null || peso <= 0) {
                continue;
            }
            sumaPonderada += nota * peso;
            sumaPesos += peso;
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

    private Integer pesoDe(DistribucionPorcentual d, ClaveComponente clave) {
        if (clave == null || d == null) {
            return null;
        }
        return switch (clave) {
            case COTIDIANO -> d.getCotidiano();
            case TAREA -> d.getTareas();
            case PROYECTO -> d.getProyectos();
            case EXAMEN -> d.getExamenes();
        };
    }
}
