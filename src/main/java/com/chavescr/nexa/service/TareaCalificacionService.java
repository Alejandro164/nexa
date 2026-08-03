package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaTareaCalificacion;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TareaCalificacion;
import com.chavescr.nexa.entity.TareaDefinicion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.TareaCalificacionRepository;
import com.chavescr.nexa.repository.TareaDefinicionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class TareaCalificacionService {

    private final TareaCalificacionRepository calificacionRepository;
    private final TareaDefinicionRepository tareaDefinicionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;

    public TareaCalificacionService(TareaCalificacionRepository calificacionRepository,
            TareaDefinicionRepository tareaDefinicionRepository, PeriodoAcademicoRepository periodoRepository,
            UsuarioRepository usuarioRepository) {
        this.calificacionRepository = calificacionRepository;
        this.tareaDefinicionRepository = tareaDefinicionRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Tareas definidas para esta sección y materia. */
    @Transactional(readOnly = true)
    public List<TareaDefinicion> listarTareasDisponibles(Long institucionId, Long nivelId, Long materiaId) {
        return tareaDefinicionRepository.findByInstitucionIdAndNivelIdAndMateriaIdOrderByFechaEntregaAsc(
                institucionId, nivelId, materiaId);
    }

    @Transactional(readOnly = true)
    public TareaDefinicion obtenerTareaDefinicion(Long institucionId, Long id) {
        return tareaDefinicionRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    /** El período activo más reciente de la institución; la evaluación de tareas se lleva a ese nivel, no diario. */
    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivo(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay un período académico activo"));
    }

    /** Una fila por cada estudiante activo de la sección, con su calificación de esa tarea en el período activo. */
    @Transactional(readOnly = true)
    public List<FilaTareaCalificacion> listarFilas(Long institucionId, Long nivelId, Long tareaDefinicionId,
            Long periodoId) {
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
        Map<Long, TareaCalificacion> registros = calificacionRepository
                .findByInstitucionIdAndTareaDefinicionIdAndPeriodoId(institucionId, tareaDefinicionId, periodoId).stream()
                .collect(Collectors.toMap(t -> t.getEstudiante().getId(), t -> t));
        return estudiantes.stream()
                .map(e -> construirFila(e, registros.get(e.getId())))
                .toList();
    }

    public FilaTareaCalificacion registrarCalificacion(Long institucionId, Long estudianteId, Long tareaDefinicionId,
            Long periodoId, Integer calificacion, String observacion) {
        TareaDefinicion tareaDefinicion = tareaDefinicionRepository.findByIdAndInstitucionId(tareaDefinicionId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        TareaCalificacion calificacionRegistro = calificacionRepository
                .findByInstitucionIdAndEstudianteIdAndTareaDefinicionIdAndPeriodoId(
                        institucionId, estudianteId, tareaDefinicionId, periodoId)
                .orElseGet(TareaCalificacion::new);

        Integer calificacionFinal = calificacion != null ? calificacion : calificacionRegistro.getCalificacion();
        if (calificacionFinal == null) {
            throw new IllegalArgumentException("Debes asignar primero una calificación");
        }
        if (calificacionFinal < 0 || calificacionFinal > 100) {
            throw new IllegalArgumentException("La calificación debe estar entre 0 y 100");
        }
        String observacionFinal = calificacionRegistro.getObservacion();
        if (observacion != null) {
            observacionFinal = observacion.isBlank() ? null : observacion.trim();
        }

        calificacionRegistro.setInstitucion(tareaDefinicion.getInstitucion());
        calificacionRegistro.setTareaDefinicion(tareaDefinicion);
        calificacionRegistro.setEstudiante(estudiante);
        calificacionRegistro.setPeriodo(periodo);
        calificacionRegistro.setCalificacion(calificacionFinal);
        calificacionRegistro.setObservacion(observacionFinal);
        calificacionRepository.save(calificacionRegistro);

        return construirFila(estudiante, calificacionRegistro);
    }

    private FilaTareaCalificacion construirFila(Usuario estudiante, TareaCalificacion registro) {
        return new FilaTareaCalificacion(estudiante,
                registro != null ? registro.getCalificacion() : null,
                registro != null ? registro.getObservacion() : null);
    }
}
