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

    public ProyectoDefinicion crearProyecto(Long institucionId, Long nivelId, Long materiaId) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodo = obtenerPeriodoActivo(institucionId);

        ProyectoDefinicion proyecto = new ProyectoDefinicion();
        proyecto.setInstitucion(nivel.getInstitucion());
        proyecto.setNivel(nivel);
        proyecto.setMateria(materia);
        proyecto.setPeriodo(periodo);
        return proyectoRepository.save(proyecto);
    }

    /** El porcentaje de cada proyecto se descuenta del 100% disponible entre los proyectos de esa sección/materia/período. */
    public ProyectoDefinicion actualizarProyecto(Long institucionId, Long id, Integer porcentaje, Integer puntosTotales) {
        ProyectoDefinicion proyecto = obtenerProyecto(institucionId, id);
        if (porcentaje != null) {
            if (porcentaje < 0 || porcentaje > 100) {
                throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
            }
            int sumaExistente = listarProyectos(institucionId, proyecto.getNivel().getId(), proyecto.getMateria().getId(),
                    proyecto.getPeriodo().getId()).stream()
                    .filter(p -> !p.getId().equals(id) && p.getPorcentaje() != null)
                    .mapToInt(ProyectoDefinicion::getPorcentaje)
                    .sum();
            if (sumaExistente + porcentaje > 100) {
                throw new IllegalArgumentException(
                        "La suma de los proyectos no puede superar 100% (actual: " + sumaExistente + "%)");
            }
            proyecto.setPorcentaje(porcentaje);
        }
        if (puntosTotales != null) {
            if (puntosTotales < 0) {
                throw new IllegalArgumentException("Los puntos totales no pueden ser negativos");
            }
            proyecto.setPuntosTotales(puntosTotales);
        }
        return proyectoRepository.save(proyecto);
    }

    public void eliminarProyecto(Long institucionId, Long id) {
        ProyectoDefinicion proyecto = obtenerProyecto(institucionId, id);
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
    public FilaNotaProyecto guardarNota(Long institucionId, Long proyectoId, Long estudianteId, Integer puntosObtenidos,
            String observacion) {
        ProyectoDefinicion proyecto = obtenerProyecto(institucionId, proyectoId);
        if (proyecto.getPuntosTotales() == null || proyecto.getPuntosTotales() <= 0) {
            throw new IllegalArgumentException("Debes definir los puntos totales del proyecto antes de calificar");
        }
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
