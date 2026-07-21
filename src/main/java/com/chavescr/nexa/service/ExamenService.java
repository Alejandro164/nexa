package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaNotaExamen;
import com.chavescr.nexa.entity.Examen;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NotaExamen;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ExamenRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NotaExamenRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MateriaRepository materiaRepository;
    private final NotaExamenRepository notaExamenRepository;
    private final UsuarioRepository usuarioRepository;

    public ExamenService(ExamenRepository examenRepository, PeriodoAcademicoRepository periodoRepository,
            MateriaRepository materiaRepository, NotaExamenRepository notaExamenRepository,
            UsuarioRepository usuarioRepository) {
        this.examenRepository = examenRepository;
        this.periodoRepository = periodoRepository;
        this.materiaRepository = materiaRepository;
        this.notaExamenRepository = notaExamenRepository;
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
    public List<Examen> listarExamenes(Long institucionId, Long periodoId, Long materiaId) {
        return examenRepository.findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByFechaAsc(
                institucionId, periodoId, materiaId);
    }

    @Transactional(readOnly = true)
    public Examen obtenerExamen(Long institucionId, Long id) {
        return examenRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));
    }

    public Examen guardarExamen(Long institucionId, Long periodoId, Long materiaId, Examen datos) {
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        Examen examen = datos.getId() != null ? obtenerExamen(institucionId, datos.getId()) : new Examen();
        examen.setInstitucion(periodo.getInstitucion());
        examen.setPeriodo(periodo);
        examen.setMateria(materia);
        examen.setTitulo(datos.getTitulo().trim());
        examen.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        examen.setTipo(datos.getTipo() != null ? datos.getTipo() : Examen.TipoExamen.EXAMEN);
        examen.setFecha(datos.getFecha());
        examen.setPorcentaje(datos.getPorcentaje());
        examen.setEstado(datos.getEstado() != null ? datos.getEstado() : Examen.EstadoExamen.PROGRAMADO);
        return examenRepository.save(examen);
    }

    public void eliminarExamen(Long institucionId, Long id) {
        Examen examen = obtenerExamen(institucionId, id);
        examenRepository.delete(examen);
    }

    @Transactional(readOnly = true)
    public List<FilaNotaExamen> listarNotas(Long institucionId, Long examenId) {
        Examen examen = obtenerExamen(institucionId, examenId);
        Map<Long, NotaExamen> notas = notaExamenRepository.findByExamenId(examen.getId()).stream()
                .collect(Collectors.toMap(n -> n.getEstudiante().getId(), n -> n));
        List<Usuario> estudiantes = usuarioRepository.findActivosByInstitucionIdAndRol(institucionId, "ROLE_ESTUDIANTE");
        return estudiantes.stream()
                .map(e -> new FilaNotaExamen(e, notas.containsKey(e.getId()) ? notas.get(e.getId()).getCalificacion() : null))
                .toList();
    }

    public void guardarNota(Long institucionId, Long examenId, Long estudianteId, Integer calificacion) {
        Examen examen = obtenerExamen(institucionId, examenId);
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        NotaExamen nota = notaExamenRepository.findByExamenIdAndEstudianteId(examen.getId(), estudianteId)
                .orElseGet(NotaExamen::new);
        nota.setExamen(examen);
        nota.setEstudiante(estudiante);
        nota.setCalificacion(calificacion);
        notaExamenRepository.save(nota);
    }
}
