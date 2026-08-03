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
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class AsistenciaService {

    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;

    public AsistenciaService(AsistenciaEstudianteRepository asistenciaRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarSeccionesActivas(Long institucionId) {
        return nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<FilaAsistencia> listarFilas(Long institucionId, Long nivelId, LocalDate fecha, Long materiaId,
            Integer numeroLeccion) {
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
        Map<Long, AsistenciaEstudiante> registros = asistenciaRepository
                .findByInstitucionIdAndNivelAcademicoIdAndFechaAndMateriaIdAndNumeroLeccion(
                        institucionId, nivelId, fecha, materiaId, numeroLeccion)
                .stream()
                .collect(Collectors.toMap(a -> a.getEstudiante().getId(), a -> a));
        return estudiantes.stream()
                .map(e -> construirFila(e, registros.get(e.getId())))
                .toList();
    }

    public FilaAsistencia registrarEstado(Long institucionId, Long estudianteId, Long nivelId, Long materiaId,
            Integer numeroLeccion, LocalDate fecha, String estado, String observaciones, Long registradoPorId) {
        NivelAcademico nivel = nivelAcademicoRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndInstitucionId(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        AsistenciaEstudiante registro = asistenciaRepository
                .findByInstitucionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                        institucionId, estudianteId, fecha, materiaId, numeroLeccion)
                .orElseGet(AsistenciaEstudiante::new);
        registro.setInstitucion(nivel.getInstitucion());
        registro.setNivelAcademico(nivel);
        registro.setEstudiante(estudiante);
        registro.setMateria(materia);
        registro.setNumeroLeccion(numeroLeccion);
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

    /**
     * Copia el estado de asistencia de cada estudiante registrado en {@code leccionOrigen} hacia
     * {@code leccionDestino}, para la misma sección/materia/fecha. Estudiantes sin registro en el
     * origen se dejan intactos. Devuelve cuántos registros se copiaron.
     */
    public int copiarDeLeccionAnterior(Long institucionId, Long nivelId, Long materiaId, LocalDate fecha,
            Integer leccionOrigen, Integer leccionDestino, Long registradoPorId) {
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
        int copiados = 0;
        for (Usuario estudiante : estudiantes) {
            AsistenciaEstudiante origen = asistenciaRepository
                    .findByInstitucionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                            institucionId, estudiante.getId(), fecha, materiaId, leccionOrigen)
                    .orElse(null);
            if (origen == null || origen.getEstado() == null) {
                continue;
            }
            registrarEstado(institucionId, estudiante.getId(), nivelId, materiaId, leccionDestino, fecha,
                    origen.getEstado().name(), origen.getObservaciones(), registradoPorId);
            copiados++;
        }
        return copiados;
    }

    private FilaAsistencia construirFila(Usuario estudiante, AsistenciaEstudiante registro) {
        return new FilaAsistencia(estudiante,
                registro != null ? registro.getEstado() : null,
                registro != null ? registro.getObservaciones() : null);
    }
}
