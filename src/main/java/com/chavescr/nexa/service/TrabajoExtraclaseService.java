package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TrabajoExtraclase;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.TrabajoExtraclaseRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class TrabajoExtraclaseService {

    private final TrabajoExtraclaseRepository trabajoRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;

    public TrabajoExtraclaseService(TrabajoExtraclaseRepository trabajoRepository,
            PeriodoAcademicoRepository periodoRepository, MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository) {
        this.trabajoRepository = trabajoRepository;
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
    public List<TrabajoExtraclase> listarTrabajos(Long institucionId, Long periodoId, Long materiaId) {
        return trabajoRepository.findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaEntregaAsc(
                institucionId, periodoId, materiaId);
    }

    @Transactional(readOnly = true)
    public TrabajoExtraclase obtenerTrabajo(Long institucionId, Long id) {
        return trabajoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo extraclase no encontrado"));
    }

    public TrabajoExtraclase guardarTrabajo(Long institucionId, Long periodoId, Long materiaId,
            Long estudianteId, TrabajoExtraclase datos) {
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        TrabajoExtraclase trabajo = datos.getId() != null
                ? obtenerTrabajo(institucionId, datos.getId())
                : new TrabajoExtraclase();
        trabajo.setInstitucion(periodo.getInstitucion());
        trabajo.setPeriodo(periodo);
        trabajo.setMateria(materia);
        trabajo.setEstudiante(estudiante);
        trabajo.setTitulo(datos.getTitulo().trim());
        trabajo.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        trabajo.setFechaEntrega(datos.getFechaEntrega());
        trabajo.setPorcentaje(datos.getPorcentaje());
        trabajo.setEstado(datos.getEstado() != null ? datos.getEstado() : TrabajoExtraclase.EstadoTrabajoExtraclase.PENDIENTE);
        trabajo.setCalificacion(datos.getCalificacion());
        return trabajoRepository.save(trabajo);
    }

    public void eliminarTrabajo(Long institucionId, Long id) {
        TrabajoExtraclase trabajo = obtenerTrabajo(institucionId, id);
        trabajoRepository.delete(trabajo);
    }
}
