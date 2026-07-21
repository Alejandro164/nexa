package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ProyectoEstudiantil;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ProyectoEstudiantilRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ProyectoEstudiantilService {

    private final ProyectoEstudiantilRepository proyectoRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;

    public ProyectoEstudiantilService(ProyectoEstudiantilRepository proyectoRepository,
            PeriodoAcademicoRepository periodoRepository, MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository) {
        this.proyectoRepository = proyectoRepository;
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
    public List<ProyectoEstudiantil> listarProyectos(Long institucionId, Long periodoId, Long materiaId) {
        return proyectoRepository.findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaInicioAsc(
                institucionId, periodoId, materiaId);
    }

    @Transactional(readOnly = true)
    public ProyectoEstudiantil obtenerProyecto(Long institucionId, Long id) {
        return proyectoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
    }

    public ProyectoEstudiantil guardarProyecto(Long institucionId, Long periodoId, Long materiaId,
            Long estudianteId, ProyectoEstudiantil datos) {
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        ProyectoEstudiantil proyecto = datos.getId() != null
                ? obtenerProyecto(institucionId, datos.getId())
                : new ProyectoEstudiantil();
        proyecto.setInstitucion(periodo.getInstitucion());
        proyecto.setPeriodo(periodo);
        proyecto.setMateria(materia);
        proyecto.setEstudiante(estudiante);
        proyecto.setTitulo(datos.getTitulo().trim());
        proyecto.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        proyecto.setFechaInicio(datos.getFechaInicio());
        proyecto.setFechaFin(datos.getFechaFin());
        proyecto.setPorcentaje(datos.getPorcentaje());
        proyecto.setEstado(datos.getEstado() != null ? datos.getEstado() : ProyectoEstudiantil.EstadoProyecto.ABIERTO);
        proyecto.setCalificacion(datos.getCalificacion());
        return proyectoRepository.save(proyecto);
    }

    public void eliminarProyecto(Long institucionId, Long id) {
        ProyectoEstudiantil proyecto = obtenerProyecto(institucionId, id);
        proyectoRepository.delete(proyecto);
    }
}
