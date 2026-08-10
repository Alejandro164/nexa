package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaNotaProyecto;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ProyectoCalificacion;
import com.chavescr.nexa.entity.ProyectoDefinicion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ProyectoCalificacionRepository;
import com.chavescr.nexa.repository.ProyectoDefinicionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ProyectoEstudiantilService {

    private final ProyectoDefinicionRepository proyectoRepository;
    private final ProyectoCalificacionRepository calificacionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;

    public ProyectoEstudiantilService(ProyectoDefinicionRepository proyectoRepository,
            ProyectoCalificacionRepository calificacionRepository, PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository) {
        this.proyectoRepository = proyectoRepository;
        this.calificacionRepository = calificacionRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** El período activo más reciente de la institución; los proyectos se numeran dentro de ese período. */
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
    public List<ProyectoDefinicion> listarProyectos(Long institucionId, Long nivelId, Long materiaId, Long periodoId) {
        return proyectoRepository.findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
                institucionId, nivelId, materiaId, periodoId);
    }

    @Transactional(readOnly = true)
    public ProyectoDefinicion obtenerProyecto(Long institucionId, Long id) {
        return proyectoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
    }

    /** Cuántos estudiantes activos tiene la sección; para mostrar "evaluados / total" junto a los proyectos. */
    @Transactional(readOnly = true)
    public int contarEstudiantesActivos(Long nivelId) {
        return usuarioRepository.findEstudiantesActivosByNivelId(nivelId).size();
    }

    /** Cuántos estudiantes distintos ya tienen una calificación registrada, por proyecto. */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarEvaluadosPorProyecto(List<Long> proyectoIds) {
        if (proyectoIds == null || proyectoIds.isEmpty()) {
            return Map.of();
        }
        return calificacionRepository.findByProyectoDefinicionIdIn(proyectoIds).stream()
                .collect(Collectors.groupingBy(c -> c.getProyectoDefinicion().getId(), Collectors.counting()));
    }

    /** Promedio de las calificaciones ya registradas, por proyecto; solo en memoria, no se persiste. */
    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPromedioPorProyecto(List<Long> proyectoIds) {
        if (proyectoIds == null || proyectoIds.isEmpty()) {
            return Map.of();
        }
        return calificacionRepository.findByProyectoDefinicionIdIn(proyectoIds).stream()
                .collect(Collectors.groupingBy(c -> c.getProyectoDefinicion().getId(),
                        Collectors.averagingInt(ProyectoCalificacion::getCalificacion)));
    }

    /**
     * A diferencia de indicadores/tareas, en los proyectos los puntos totales son obligatorios: la
     * calificación siempre se deriva de puntosObtenidos/puntosTotales, nunca se ingresa directamente.
     */
    public ProyectoDefinicion guardarProyecto(Long institucionId, Long nivelId, Long materiaId,
            ProyectoDefinicion datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodoActivo = obtenerPeriodoActivo(institucionId);

        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new IllegalArgumentException("Debes indicar el nombre del proyecto");
        }
        if (datos.getPorcentaje() == null) {
            throw new IllegalArgumentException("Debes indicar el porcentaje del proyecto");
        }
        if (datos.getPorcentaje() < 0 || datos.getPorcentaje() > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        int sumaExistente = listarProyectos(institucionId, nivelId, materiaId, periodoActivo.getId()).stream()
                .filter(p -> !p.getId().equals(datos.getId()))
                .mapToInt(ProyectoDefinicion::getPorcentaje)
                .sum();
        if (sumaExistente + datos.getPorcentaje() > 100) {
            throw new IllegalArgumentException(
                    "La suma de los proyectos no puede superar 100% (actual: " + sumaExistente + "%)");
        }

        if (datos.getPuntosTotales() == null || datos.getPuntosTotales() <= 0) {
            throw new IllegalArgumentException("Debes indicar los puntos totales del proyecto");
        }

        ProyectoDefinicion proyecto = datos.getId() != null
                ? obtenerProyecto(institucionId, datos.getId())
                : new ProyectoDefinicion();
        proyecto.setInstitucion(nivel.getInstitucion());
        proyecto.setNivel(nivel);
        proyecto.setMateria(materia);
        if (proyecto.getPeriodo() == null) {
            proyecto.setPeriodo(periodoActivo);
        }
        proyecto.setTitulo(datos.getTitulo().trim());
        proyecto.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        proyecto.setPorcentaje(datos.getPorcentaje());
        proyecto.setPuntosTotales(datos.getPuntosTotales());
        return proyectoRepository.save(proyecto);
    }

    public void eliminarProyecto(Long institucionId, Long id) {
        ProyectoDefinicion proyecto = obtenerProyecto(institucionId, id);
        if (calificacionRepository.existsByProyectoDefinicionId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el proyecto ya tiene calificaciones registradas");
        }
        proyectoRepository.delete(proyecto);
    }

    @Transactional(readOnly = true)
    public List<FilaNotaProyecto> listarNotas(Long institucionId, Long proyectoId) {
        ProyectoDefinicion proyecto = obtenerProyecto(institucionId, proyectoId);
        Map<Long, ProyectoCalificacion> notas = calificacionRepository.findByProyectoDefinicionId(proyecto.getId()).stream()
                .collect(Collectors.toMap(n -> n.getEstudiante().getId(), n -> n));
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(proyecto.getNivel().getId());
        return estudiantes.stream()
                .map(e -> construirFila(e, notas.get(e.getId())))
                .toList();
    }

    /** La calificación (0-100) se deriva de puntosObtenidos/puntosTotales del proyecto; no se ingresa directamente. */
    public FilaNotaProyecto registrarCalificacion(Long institucionId, Long proyectoId, Long estudianteId,
            Integer puntosObtenidos, String observacion) {
        ProyectoDefinicion proyecto = obtenerProyecto(institucionId, proyectoId);
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        ProyectoCalificacion nota = calificacionRepository
                .findByProyectoDefinicionIdAndEstudianteId(proyecto.getId(), estudianteId)
                .orElseGet(ProyectoCalificacion::new);

        Integer puntosFinal = puntosObtenidos != null ? puntosObtenidos : nota.getPuntosObtenidos();
        if (puntosFinal == null) {
            throw new IllegalArgumentException("Debes asignar primero los puntos obtenidos");
        }
        if (puntosFinal < 0 || puntosFinal > proyecto.getPuntosTotales()) {
            throw new IllegalArgumentException("Los puntos obtenidos deben estar entre 0 y " + proyecto.getPuntosTotales());
        }
        String observacionFinal = nota.getObservacion();
        if (observacion != null) {
            observacionFinal = observacion.isBlank() ? null : observacion.trim();
        }

        nota.setProyectoDefinicion(proyecto);
        nota.setEstudiante(estudiante);
        nota.setPuntosObtenidos(puntosFinal);
        nota.setCalificacion((int) Math.round(puntosFinal * 100.0 / proyecto.getPuntosTotales()));
        nota.setObservacion(observacionFinal);
        calificacionRepository.save(nota);

        return construirFila(estudiante, nota);
    }

    private FilaNotaProyecto construirFila(Usuario estudiante, ProyectoCalificacion registro) {
        return new FilaNotaProyecto(estudiante,
                registro != null ? registro.getPuntosObtenidos() : null,
                registro != null ? registro.getCalificacion() : null,
                registro != null ? registro.getObservacion() : null);
    }
}
