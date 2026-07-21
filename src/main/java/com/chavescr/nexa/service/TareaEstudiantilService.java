package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TareaEstudiantil;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.TareaEstudiantilRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class TareaEstudiantilService {

    private final TareaEstudiantilRepository tareaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;

    public TareaEstudiantilService(TareaEstudiantilRepository tareaRepository,
            PeriodoAcademicoRepository periodoRepository, MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository) {
        this.tareaRepository = tareaRepository;
        this.periodoRepository = periodoRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodosActivos(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarEstudiantesActivos(Long institucionId) {
        return usuarioRepository.findActivosByInstitucionIdAndRol(institucionId, "ROLE_ESTUDIANTE");
    }

    @Transactional(readOnly = true)
    public List<TareaEstudiantil> listarTareas(Long institucionId, Long periodoId, Long materiaId) {
        return tareaRepository.findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaEntregaAsc(
                institucionId, periodoId, materiaId);
    }

    @Transactional(readOnly = true)
    public TareaEstudiantil obtenerTarea(Long institucionId, Long id) {
        return tareaRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    public TareaEstudiantil guardarTarea(Long institucionId, Long periodoId, Long materiaId,
            Long estudianteId, TareaEstudiantil datos) {
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        TareaEstudiantil tarea = datos.getId() != null
                ? obtenerTarea(institucionId, datos.getId())
                : new TareaEstudiantil();
        tarea.setInstitucion(periodo.getInstitucion());
        tarea.setPeriodo(periodo);
        tarea.setMateria(materia);
        tarea.setEstudiante(estudiante);
        tarea.setTitulo(datos.getTitulo().trim());
        tarea.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        tarea.setFechaEntrega(datos.getFechaEntrega());
        tarea.setPorcentaje(datos.getPorcentaje());
        tarea.setEstado(datos.getEstado() != null ? datos.getEstado() : TareaEstudiantil.EstadoTareaEstudiantil.PENDIENTE);
        tarea.setCalificacion(datos.getCalificacion());
        return tareaRepository.save(tarea);
    }

    public void eliminarTarea(Long institucionId, Long id) {
        TareaEstudiantil tarea = obtenerTarea(institucionId, id);
        tareaRepository.delete(tarea);
    }
}
