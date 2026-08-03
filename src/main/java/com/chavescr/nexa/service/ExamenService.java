package com.chavescr.nexa.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaNotaExamen;
import com.chavescr.nexa.entity.Examen;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.NotaExamen;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ExamenRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.NotaExamenRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final NotaExamenRepository notaExamenRepository;
    private final UsuarioRepository usuarioRepository;

    public ExamenService(ExamenRepository examenRepository, PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository,
            NotaExamenRepository notaExamenRepository, UsuarioRepository usuarioRepository) {
        this.examenRepository = examenRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.notaExamenRepository = notaExamenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** El período activo más reciente de la institución; las pruebas se numeran dentro de ese período. */
    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivo(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay un período académico activo"));
    }

    @Transactional(readOnly = true)
    public List<Examen> listarExamenes(Long institucionId, Long nivelId, Long materiaId, Long periodoId) {
        return examenRepository.findByInstitucionIdAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
                institucionId, nivelId, materiaId, periodoId);
    }

    @Transactional(readOnly = true)
    public Examen obtenerExamen(Long institucionId, Long id) {
        return examenRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));
    }

    public Examen crearPrueba(Long institucionId, Long nivelId, Long materiaId) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodo = obtenerPeriodoActivo(institucionId);

        Examen examen = new Examen();
        examen.setInstitucion(nivel.getInstitucion());
        examen.setNivel(nivel);
        examen.setMateria(materia);
        examen.setPeriodo(periodo);
        return examenRepository.save(examen);
    }

    /** El porcentaje de cada prueba se descuenta del 100% disponible entre las pruebas de esa sección/materia/período. */
    public Examen actualizarPrueba(Long institucionId, Long id, Integer porcentaje, Integer puntosTotales) {
        Examen examen = obtenerExamen(institucionId, id);
        if (porcentaje != null) {
            if (porcentaje < 0 || porcentaje > 100) {
                throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
            }
            int sumaExistente = listarExamenes(institucionId, examen.getNivel().getId(), examen.getMateria().getId(),
                    examen.getPeriodo().getId()).stream()
                    .filter(e -> !e.getId().equals(id) && e.getPorcentaje() != null)
                    .mapToInt(Examen::getPorcentaje)
                    .sum();
            if (sumaExistente + porcentaje > 100) {
                throw new IllegalArgumentException(
                        "La suma de las pruebas no puede superar 100% (actual: " + sumaExistente + "%)");
            }
            examen.setPorcentaje(porcentaje);
        }
        if (puntosTotales != null) {
            if (puntosTotales < 0) {
                throw new IllegalArgumentException("Los puntos totales no pueden ser negativos");
            }
            examen.setPuntosTotales(puntosTotales);
        }
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
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(examen.getNivel().getId());
        return estudiantes.stream()
                .map(e -> construirFila(e, notas.get(e.getId())))
                .toList();
    }

    /** La calificación (0-100) se deriva de puntosObtenidos/puntosTotales de la prueba; no se ingresa directamente. */
    public FilaNotaExamen guardarNota(Long institucionId, Long examenId, Long estudianteId, Integer puntosObtenidos,
            String observacion) {
        Examen examen = obtenerExamen(institucionId, examenId);
        if (examen.getPuntosTotales() == null || examen.getPuntosTotales() <= 0) {
            throw new IllegalArgumentException("Debes definir los puntos totales de la prueba antes de calificar");
        }
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        NotaExamen nota = notaExamenRepository.findByExamenIdAndEstudianteId(examen.getId(), estudianteId)
                .orElseGet(NotaExamen::new);

        Integer puntosFinal = puntosObtenidos != null ? puntosObtenidos : nota.getPuntosObtenidos();
        if (puntosFinal == null) {
            throw new IllegalArgumentException("Debes asignar primero los puntos obtenidos");
        }
        if (puntosFinal < 0 || puntosFinal > examen.getPuntosTotales()) {
            throw new IllegalArgumentException("Los puntos obtenidos deben estar entre 0 y " + examen.getPuntosTotales());
        }
        String observacionFinal = nota.getObservacion();
        if (observacion != null) {
            observacionFinal = observacion.isBlank() ? null : observacion.trim();
        }

        nota.setExamen(examen);
        nota.setEstudiante(estudiante);
        nota.setPuntosObtenidos(puntosFinal);
        nota.setCalificacion((int) Math.round(puntosFinal * 100.0 / examen.getPuntosTotales()));
        nota.setObservacion(observacionFinal);
        notaExamenRepository.save(nota);

        return construirFila(estudiante, nota);
    }

    private FilaNotaExamen construirFila(Usuario estudiante, NotaExamen registro) {
        return new FilaNotaExamen(estudiante,
                registro != null ? registro.getPuntosObtenidos() : null,
                registro != null ? registro.getCalificacion() : null,
                registro != null ? registro.getObservacion() : null);
    }
}
