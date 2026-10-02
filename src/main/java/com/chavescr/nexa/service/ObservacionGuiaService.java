package com.chavescr.nexa.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.DocenteGuia;
import com.chavescr.nexa.entity.ObservacionGuia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.DocenteGuiaRepository;
import com.chavescr.nexa.repository.ObservacionGuiaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(rollbackFor = Exception.class)
public class ObservacionGuiaService {

    private final ObservacionGuiaRepository observacionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final DocenteGuiaRepository docenteGuiaRepository;

    public ObservacionGuiaService(ObservacionGuiaRepository observacionRepository,
            PeriodoAcademicoRepository periodoRepository, UsuarioRepository usuarioRepository,
            DireccionRepository direccionRepository, DocenteGuiaRepository docenteGuiaRepository) {
        this.observacionRepository = observacionRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
        this.docenteGuiaRepository = docenteGuiaRepository;
    }

    /**
     * Crea, actualiza o borra la observación del período. Un texto vacío elimina el registro.
     * Dirección y administrador pueden escribir en cualquier sección; el docente, solo si es
     * el profesor guía de la sección actual del estudiante.
     */
    public void guardar(Long direccionId, Long autorId, boolean supervision, Long estudianteId, Long periodoId,
            String texto) {
        PeriodoAcademico periodo = periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        if (estudiante.getNivelAcademico() == null) {
            throw new IllegalArgumentException("El estudiante no tiene sección asignada");
        }
        if (!supervision && !esGuiaDe(direccionId, autorId, estudiante.getNivelAcademico().getId())) {
            throw new IllegalArgumentException("Solo el docente guía de la sección puede registrar la observación");
        }

        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() > ObservacionGuia.TEXTO_MAX) {
            throw new IllegalArgumentException(
                    "La observación no puede superar " + ObservacionGuia.TEXTO_MAX + " caracteres");
        }

        ObservacionGuia existente = observacionRepository
                .findByDireccionIdAndEstudianteIdAndPeriodoId(direccionId, estudianteId, periodoId)
                .orElse(null);
        if (limpio.isEmpty()) {
            if (existente != null) {
                observacionRepository.delete(existente);
            }
            return;
        }
        if (existente != null) {
            aplicar(existente, limpio, autorId);
            observacionRepository.save(existente);
            return;
        }

        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
        ObservacionGuia nueva = new ObservacionGuia();
        nueva.setDireccion(direccion);
        nueva.setEstudiante(estudiante);
        nueva.setPeriodo(periodo);
        aplicar(nueva, limpio, autorId);
        try {
            observacionRepository.saveAndFlush(nueva);
        } catch (DataIntegrityViolationException e) {
            ObservacionGuia concurrente = observacionRepository
                    .findByDireccionIdAndEstudianteIdAndPeriodoId(direccionId, estudianteId, periodoId)
                    .orElseThrow(() -> e);
            aplicar(concurrente, limpio, autorId);
            observacionRepository.save(concurrente);
        }
    }

    private boolean esGuiaDe(Long direccionId, Long autorId, Long nivelId) {
        if (autorId == null) {
            return false;
        }
        return docenteGuiaRepository.findByDireccionIdAndNivelId(direccionId, nivelId)
                .map(DocenteGuia::getDocente)
                .map(docente -> autorId.equals(docente.getId()))
                .orElse(false);
    }

    private void aplicar(ObservacionGuia observacion, String texto, Long autorId) {
        observacion.setTexto(texto);
        observacion.setActualizadoEn(LocalDateTime.now());
        if (autorId != null) {
            usuarioRepository.findById(autorId).ifPresent(observacion::setRegistradoPor);
        }
    }
}
