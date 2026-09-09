package com.chavescr.nexa.service;

import java.util.LinkedHashMap;
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

    /** Igual que {@link #obtenerPeriodoActivo}, pero null en vez de lanzar si no hay ninguno activo. */
    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodoActivoOpcional(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId).stream()
                .findFirst()
                .orElse(null);
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

    /** Cuántos estudiantes activos tiene la sección; para mostrar "evaluados / total" junto a las pruebas. */
    @Transactional(readOnly = true)
    public int contarEstudiantesActivos(Long nivelId) {
        return usuarioRepository.findEstudiantesActivosByNivelId(nivelId).size();
    }

    /** Cuántos estudiantes distintos ya tienen una calificación registrada, por prueba. */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarEvaluadosPorExamen(List<Long> examenIds) {
        if (examenIds == null || examenIds.isEmpty()) {
            return Map.of();
        }
        return notaExamenRepository.findByExamenIdIn(examenIds).stream()
                .collect(Collectors.groupingBy(n -> n.getExamen().getId(), Collectors.counting()));
    }

    /** Promedio de las calificaciones ya registradas, por prueba; solo en memoria, no se persiste. */
    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPromedioPorExamen(List<Long> examenIds) {
        if (examenIds == null || examenIds.isEmpty()) {
            return Map.of();
        }
        return notaExamenRepository.findByExamenIdIn(examenIds).stream()
                .collect(Collectors.groupingBy(n -> n.getExamen().getId(),
                        Collectors.averagingInt(NotaExamen::getCalificacion)));
    }

    /**
     * Peso efectivo de cada prueba dentro del componente (base 100).
     * Las de porcentaje fijo conservan su valor; el resto (100 − suma de fijos) se reparte
     * equitativamente entre las ponderadas (porcentaje null).
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPesosEfectivos(List<Examen> examenes) {
        Map<Long, Double> pesos = new LinkedHashMap<>();
        if (examenes == null || examenes.isEmpty()) {
            return pesos;
        }
        int sumaFijos = examenes.stream()
                .filter(e -> e.getPorcentaje() != null)
                .mapToInt(Examen::getPorcentaje)
                .sum();
        long ponderadas = examenes.stream().filter(Examen::isPonderado).count();
        double resto = Math.max(0, 100 - sumaFijos);
        double pesoPonderado = ponderadas == 0 ? 0 : resto / ponderadas;
        for (Examen examen : examenes) {
            pesos.put(examen.getId(),
                    examen.isPonderado() ? pesoPonderado : examen.getPorcentaje().doubleValue());
        }
        return pesos;
    }

    /**
     * En las pruebas los puntos totales son obligatorios: la calificación siempre se deriva de
     * puntosObtenidos/puntosTotales. El porcentaje sí puede ser ponderado (null): el sistema
     * reparte el resto hasta 100%.
     */
    public Examen guardarPrueba(Long institucionId, Long nivelId, Long materiaId, Examen datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        PeriodoAcademico periodoActivo = obtenerPeriodoActivo(institucionId);

        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new IllegalArgumentException("Debes indicar el nombre de la prueba");
        }
        if (datos.getPuntosTotales() == null || datos.getPuntosTotales() <= 0) {
            throw new IllegalArgumentException("Debes indicar los puntos totales de la prueba");
        }

        List<Examen> existentes = listarExamenes(institucionId, nivelId, materiaId, periodoActivo.getId());
        int sumaFijosOtros = existentes.stream()
                .filter(e -> datos.getId() == null || !e.getId().equals(datos.getId()))
                .filter(e -> e.getPorcentaje() != null)
                .mapToInt(Examen::getPorcentaje)
                .sum();
        long ponderadasOtras = existentes.stream()
                .filter(e -> datos.getId() == null || !e.getId().equals(datos.getId()))
                .filter(Examen::isPonderado)
                .count();
        Integer porcentaje = datos.getPorcentaje();
        if (porcentaje != null) {
            if (porcentaje < 1 || porcentaje > 100) {
                throw new IllegalArgumentException("El porcentaje debe estar entre 1 y 100");
            }
            if (sumaFijosOtros + porcentaje > 100) {
                throw new IllegalArgumentException(
                        "La suma de los porcentajes fijos no puede superar 100% (disponible: "
                                + (100 - sumaFijosOtros) + "%)");
            }
            if (ponderadasOtras > 0 && sumaFijosOtros + porcentaje >= 100) {
                throw new IllegalArgumentException(
                        "Debes dejar un porcentaje disponible para las pruebas ponderadas (máximo: "
                                + Math.max(0, 99 - sumaFijosOtros) + "%)");
            }
        } else if (sumaFijosOtros >= 100) {
            throw new IllegalArgumentException(
                    "El 100% ya está asignado a pruebas con porcentaje fijo. Reduce una de ellas para poder ponderar esta.");
        }

        Examen examen = datos.getId() != null
                ? obtenerExamen(institucionId, datos.getId())
                : new Examen();
        examen.setInstitucion(nivel.getInstitucion());
        examen.setNivel(nivel);
        examen.setMateria(materia);
        if (examen.getPeriodo() == null) {
            examen.setPeriodo(periodoActivo);
        }
        examen.setTitulo(datos.getTitulo().trim());
        examen.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        examen.setPorcentaje(porcentaje);
        examen.setPuntosTotales(datos.getPuntosTotales());
        return examenRepository.save(examen);
    }

    public void eliminarExamen(Long institucionId, Long id) {
        Examen examen = obtenerExamen(institucionId, id);
        if (notaExamenRepository.existsByExamenId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: la prueba ya tiene calificaciones registradas");
        }
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
    public FilaNotaExamen registrarCalificacion(Long institucionId, Long examenId, Long estudianteId, Integer puntosObtenidos,
            String observacion) {
        Examen examen = obtenerExamen(institucionId, examenId);
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
