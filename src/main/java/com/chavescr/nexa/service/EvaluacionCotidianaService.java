package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.EvaluacionCotidiana;
import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.EvaluacionCotidianaRepository;
import com.chavescr.nexa.repository.IndicadorCotidianoRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class EvaluacionCotidianaService {

    private final EvaluacionCotidianaRepository evaluacionRepository;
    private final IndicadorCotidianoRepository indicadorRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;

    public EvaluacionCotidianaService(EvaluacionCotidianaRepository evaluacionRepository,
            IndicadorCotidianoRepository indicadorRepository, NivelAcademicoRepository nivelRepository,
            MateriaRepository materiaRepository, PeriodoAcademicoRepository periodoRepository,
            UsuarioRepository usuarioRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.indicadorRepository = indicadorRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarNivelesActivos(Long institucionId) {
        return nivelRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    /** Indicadores de la materia en el período académico activo más reciente. */
    @Transactional(readOnly = true)
    public List<IndicadorCotidiano> listarIndicadoresDisponibles(Long institucionId, Long materiaId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId).stream()
                .findFirst()
                .map(periodo -> indicadorRepository.findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByIdAsc(
                        institucionId, periodo.getId(), materiaId))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarEstudiantesDeSeccion(Long nivelId) {
        return usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
    }

    @Transactional(readOnly = true)
    public List<EvaluacionCotidiana> listarEvaluaciones(Long institucionId, Long nivelId, Long materiaId) {
        return evaluacionRepository.findByInstitucionIdAndNivelIdAndMateriaId(institucionId, nivelId, materiaId);
    }

    @Transactional(readOnly = true)
    public EvaluacionCotidiana obtenerEvaluacion(Long institucionId, Long id) {
        return evaluacionRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));
    }

    public EvaluacionCotidiana guardarEvaluacion(Long institucionId, Long indicadorId, Long estudianteId,
            EvaluacionCotidiana datos) {
        IndicadorCotidiano indicador = indicadorRepository.findByIdAndInstitucionId(indicadorId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Indicador no encontrado"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        EvaluacionCotidiana evaluacion = datos.getId() != null
                ? obtenerEvaluacion(institucionId, datos.getId())
                : new EvaluacionCotidiana();
        evaluacion.setInstitucion(indicador.getInstitucion());
        evaluacion.setIndicador(indicador);
        evaluacion.setEstudiante(estudiante);
        evaluacion.setCalificacion(datos.getCalificacion());
        evaluacion.setObservacion(datos.getObservacion() != null ? datos.getObservacion().trim() : null);
        evaluacion.setFecha(datos.getFecha());
        return evaluacionRepository.save(evaluacion);
    }

    public void eliminarEvaluacion(Long institucionId, Long id) {
        EvaluacionCotidiana evaluacion = obtenerEvaluacion(institucionId, id);
        evaluacionRepository.delete(evaluacion);
    }
}
