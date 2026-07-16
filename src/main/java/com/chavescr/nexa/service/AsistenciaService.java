package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaAsistencia;
import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class AsistenciaService {

    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final UsuarioRepository usuarioRepository;

    public AsistenciaService(AsistenciaEstudianteRepository asistenciaRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            UsuarioRepository usuarioRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarSeccionesActivas(Long institucionId) {
        return nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<FilaAsistencia> listarFilas(Long institucionId, Long nivelId, LocalDate fecha) {
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
        Map<Long, AsistenciaEstudiante> registros = asistenciaRepository
                .findByInstitucionIdAndNivelAcademicoIdAndFecha(institucionId, nivelId, fecha).stream()
                .collect(Collectors.toMap(a -> a.getEstudiante().getId(), a -> a));
        return estudiantes.stream()
                .map(e -> construirFila(e, registros.get(e.getId())))
                .toList();
    }

    public FilaAsistencia registrarEstado(Long institucionId, Long estudianteId, Long nivelId, LocalDate fecha,
            String estado, String observaciones, Long registradoPorId) {
        NivelAcademico nivel = nivelAcademicoRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        AsistenciaEstudiante registro = asistenciaRepository
                .findByInstitucionIdAndEstudianteIdAndFecha(institucionId, estudianteId, fecha)
                .orElseGet(AsistenciaEstudiante::new);
        registro.setInstitucion(nivel.getInstitucion());
        registro.setNivelAcademico(nivel);
        registro.setEstudiante(estudiante);
        registro.setFecha(fecha);

        EstadoAsistencia estadoFinal = registro.getEstado();
        if (estado != null && !estado.isBlank()) {
            estadoFinal = EstadoAsistencia.valueOf(estado);
        }
        String observacionesFinal = registro.getObservaciones();
        if (observaciones != null) {
            observacionesFinal = observaciones.isBlank() ? null : observaciones.trim();
        }
        if (estadoFinal == EstadoAsistencia.JUSTIFICADA && (observacionesFinal == null || observacionesFinal.isBlank())) {
            throw new IllegalArgumentException("Debes agregar una nota para justificar la inasistencia");
        }
        registro.setEstado(estadoFinal);
        registro.setObservaciones(observacionesFinal);

        if (registradoPorId != null) {
            usuarioRepository.findById(registradoPorId).ifPresent(registro::setRegistradoPor);
        }
        registro.setActualizadoEn(LocalDateTime.now());
        asistenciaRepository.save(registro);

        return construirFila(estudiante, registro);
    }

    private FilaAsistencia construirFila(Usuario estudiante, AsistenciaEstudiante registro) {
        return new FilaAsistencia(estudiante,
                registro != null ? registro.getEstado() : null,
                registro != null ? registro.getObservaciones() : null);
    }
}
