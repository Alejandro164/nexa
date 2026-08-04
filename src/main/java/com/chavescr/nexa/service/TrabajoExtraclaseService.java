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

    public TrabajoDefinicion crearTrabajo(Long institucionId, Long nivelId, Long materiaId) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodo = obtenerPeriodoActivo(institucionId);

        TrabajoDefinicion trabajo = new TrabajoDefinicion();
        trabajo.setInstitucion(nivel.getInstitucion());
        trabajo.setNivel(nivel);
        trabajo.setMateria(materia);
        trabajo.setPeriodo(periodo);
        return trabajoRepository.save(trabajo);
    }

    /** El porcentaje de cada trabajo se descuenta del 100% disponible entre los trabajos de esa sección/materia/período. */
    public TrabajoDefinicion actualizarTrabajo(Long institucionId, Long id, Integer porcentaje, Integer puntosTotales) {
        TrabajoDefinicion trabajo = obtenerTrabajo(institucionId, id);
        if (porcentaje != null) {
            if (porcentaje < 0 || porcentaje > 100) {
                throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
            }
            int sumaExistente = listarTrabajos(institucionId, trabajo.getNivel().getId(), trabajo.getMateria().getId(),
                    trabajo.getPeriodo().getId()).stream()
                    .filter(t -> !t.getId().equals(id) && t.getPorcentaje() != null)
                    .mapToInt(TrabajoDefinicion::getPorcentaje)
                    .sum();
            if (sumaExistente + porcentaje > 100) {
                throw new IllegalArgumentException(
                        "La suma de los trabajos no puede superar 100% (actual: " + sumaExistente + "%)");
            }
            trabajo.setPorcentaje(porcentaje);
        }
        if (puntosTotales != null) {
            if (puntosTotales < 0) {
                throw new IllegalArgumentException("Los puntos totales no pueden ser negativos");
            }
            trabajo.setPuntosTotales(puntosTotales);
        }
        return trabajoRepository.save(trabajo);
    }

    public void eliminarTrabajo(Long institucionId, Long id) {
        TrabajoDefinicion trabajo = obtenerTrabajo(institucionId, id);
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
    public FilaNotaTrabajo guardarNota(Long institucionId, Long trabajoId, Long estudianteId, Integer puntosObtenidos,
            String observacion) {
        TrabajoDefinicion trabajo = obtenerTrabajo(institucionId, trabajoId);
        if (trabajo.getPuntosTotales() == null || trabajo.getPuntosTotales() <= 0) {
            throw new IllegalArgumentException("Debes definir los puntos totales del trabajo antes de calificar");
        }
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
