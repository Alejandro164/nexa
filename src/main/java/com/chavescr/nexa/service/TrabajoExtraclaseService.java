package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaNotaTrabajo;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TrabajoCalificacion;
import com.chavescr.nexa.entity.TrabajoDefinicion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.TrabajoCalificacionRepository;
import com.chavescr.nexa.repository.TrabajoDefinicionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class TrabajoExtraclaseService {

    private final TrabajoDefinicionRepository trabajoRepository;
    private final TrabajoCalificacionRepository calificacionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;

    public TrabajoExtraclaseService(TrabajoDefinicionRepository trabajoRepository,
            TrabajoCalificacionRepository calificacionRepository, PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository) {
        this.trabajoRepository = trabajoRepository;
        this.calificacionRepository = calificacionRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** El período activo más reciente de la institución; los trabajos se numeran dentro de ese período. */
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

    @Transactional(readOnly = true)
    public List<TrabajoDefinicion> listarTrabajos(Long institucionId, Long nivelId, Long materiaId, Long periodoId) {
        return trabajoRepository.findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
                institucionId, nivelId, materiaId, periodoId);
    }

    @Transactional(readOnly = true)
    public TrabajoDefinicion obtenerTrabajo(Long institucionId, Long id) {
        return trabajoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo extraclase no encontrado"));
    }

    /** Cuántos estudiantes activos tiene la sección; para mostrar "evaluados / total" junto a los trabajos. */
    @Transactional(readOnly = true)
    public int contarEstudiantesActivos(Long nivelId) {
        return usuarioRepository.findEstudiantesActivosByNivelId(nivelId).size();
    }

    /** Cuántos estudiantes distintos ya tienen una calificación registrada, por trabajo. */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarEvaluadosPorTrabajo(List<Long> trabajoIds) {
        if (trabajoIds == null || trabajoIds.isEmpty()) {
            return Map.of();
        }
        return calificacionRepository.findByTrabajoDefinicionIdIn(trabajoIds).stream()
                .collect(Collectors.groupingBy(c -> c.getTrabajoDefinicion().getId(), Collectors.counting()));
    }

    /**
     * A diferencia de indicadores/tareas, en los trabajos extraclase los puntos totales son
     * obligatorios: la calificación siempre se deriva de puntosObtenidos/puntosTotales, nunca se
     * ingresa directamente.
     */
    public TrabajoDefinicion guardarTrabajo(Long institucionId, Long nivelId, Long materiaId, TrabajoDefinicion datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodoActivo = obtenerPeriodoActivo(institucionId);

        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new IllegalArgumentException("Debes indicar el nombre del trabajo");
        }
        if (datos.getPorcentaje() == null) {
            throw new IllegalArgumentException("Debes indicar el porcentaje del trabajo");
        }
        if (datos.getPorcentaje() < 0 || datos.getPorcentaje() > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        int sumaExistente = listarTrabajos(institucionId, nivelId, materiaId, periodoActivo.getId()).stream()
                .filter(t -> !t.getId().equals(datos.getId()))
                .mapToInt(TrabajoDefinicion::getPorcentaje)
                .sum();
        if (sumaExistente + datos.getPorcentaje() > 100) {
            throw new IllegalArgumentException(
                    "La suma de los trabajos no puede superar 100% (actual: " + sumaExistente + "%)");
        }

        if (datos.getPuntosTotales() == null || datos.getPuntosTotales() <= 0) {
            throw new IllegalArgumentException("Debes indicar los puntos totales del trabajo");
        }

        TrabajoDefinicion trabajo = datos.getId() != null
                ? obtenerTrabajo(institucionId, datos.getId())
                : new TrabajoDefinicion();
        trabajo.setInstitucion(nivel.getInstitucion());
        trabajo.setNivel(nivel);
        trabajo.setMateria(materia);
        if (trabajo.getPeriodo() == null) {
            trabajo.setPeriodo(periodoActivo);
        }
        trabajo.setTitulo(datos.getTitulo().trim());
        trabajo.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        trabajo.setPorcentaje(datos.getPorcentaje());
        trabajo.setPuntosTotales(datos.getPuntosTotales());
        return trabajoRepository.save(trabajo);
    }

    public void eliminarTrabajo(Long institucionId, Long id) {
        TrabajoDefinicion trabajo = obtenerTrabajo(institucionId, id);
        if (calificacionRepository.existsByTrabajoDefinicionId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el trabajo ya tiene calificaciones registradas");
        }
        trabajoRepository.delete(trabajo);
    }

    @Transactional(readOnly = true)
    public List<FilaNotaTrabajo> listarNotas(Long institucionId, Long trabajoId) {
        TrabajoDefinicion trabajo = obtenerTrabajo(institucionId, trabajoId);
        Map<Long, TrabajoCalificacion> notas = calificacionRepository.findByTrabajoDefinicionId(trabajo.getId()).stream()
                .collect(Collectors.toMap(n -> n.getEstudiante().getId(), n -> n));
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(trabajo.getNivel().getId());
        return estudiantes.stream()
                .map(e -> construirFila(e, notas.get(e.getId())))
                .toList();
    }

    /** La calificación (0-100) se deriva de puntosObtenidos/puntosTotales del trabajo; no se ingresa directamente. */
    public FilaNotaTrabajo registrarCalificacion(Long institucionId, Long trabajoId, Long estudianteId, Integer puntosObtenidos,
            String observacion) {
        TrabajoDefinicion trabajo = obtenerTrabajo(institucionId, trabajoId);
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        TrabajoCalificacion nota = calificacionRepository
                .findByTrabajoDefinicionIdAndEstudianteId(trabajo.getId(), estudianteId)
                .orElseGet(TrabajoCalificacion::new);

        Integer puntosFinal = puntosObtenidos != null ? puntosObtenidos : nota.getPuntosObtenidos();
        if (puntosFinal == null) {
            throw new IllegalArgumentException("Debes asignar primero los puntos obtenidos");
        }
        if (puntosFinal < 0 || puntosFinal > trabajo.getPuntosTotales()) {
            throw new IllegalArgumentException("Los puntos obtenidos deben estar entre 0 y " + trabajo.getPuntosTotales());
        }
        String observacionFinal = nota.getObservacion();
        if (observacion != null) {
            observacionFinal = observacion.isBlank() ? null : observacion.trim();
        }

        nota.setTrabajoDefinicion(trabajo);
        nota.setEstudiante(estudiante);
        nota.setPuntosObtenidos(puntosFinal);
        nota.setCalificacion((int) Math.round(puntosFinal * 100.0 / trabajo.getPuntosTotales()));
        nota.setObservacion(observacionFinal);
        calificacionRepository.save(nota);

        return construirFila(estudiante, nota);
    }

    private FilaNotaTrabajo construirFila(Usuario estudiante, TrabajoCalificacion registro) {
        return new FilaNotaTrabajo(estudiante,
                registro != null ? registro.getPuntosObtenidos() : null,
                registro != null ? registro.getCalificacion() : null,
                registro != null ? registro.getObservacion() : null);
    }
}
