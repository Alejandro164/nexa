package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DocenteGuia;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DocenteGuiaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class DocenteGuiaService {

    private final DocenteGuiaRepository docenteGuiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final DireccionRepository direccionRepository;

    public DocenteGuiaService(DocenteGuiaRepository docenteGuiaRepository,
            UsuarioRepository usuarioRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            DireccionRepository direccionRepository) {
        this.docenteGuiaRepository = docenteGuiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<NivelAcademico> listarSecciones(Long direccionId, Long docenteId) {
        return docenteGuiaRepository
                .findByDireccionIdAndDocenteIdOrderByNivel_GradoAscNivel_SeccionAsc(direccionId, docenteId)
                .stream()
                .map(DocenteGuia::getNivel)
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Long> listarNivelIds(Long direccionId, Long docenteId) {
        return listarSecciones(direccionId, docenteId).stream().map(NivelAcademico::getId).toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean esProfesorGuia(Long direccionId, Long docenteId) {
        return !listarNivelIds(direccionId, docenteId).isEmpty();
    }

    /**
     * Secciones activas, más las ya asignadas aunque estén inactivas.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<NivelAcademico> catalogoParaFormulario(Long direccionId, Long docenteId) {
        List<NivelAcademico> activas = nivelAcademicoRepository
                .findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
        if (docenteId == null) {
            return activas;
        }
        Map<Long, NivelAcademico> porId = new LinkedHashMap<>();
        for (NivelAcademico nivel : activas) {
            porId.put(nivel.getId(), nivel);
        }
        for (NivelAcademico nivel : listarSecciones(direccionId, docenteId)) {
            porId.putIfAbsent(nivel.getId(), nivel);
        }
        return new ArrayList<>(porId.values());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, List<NivelAcademico>> mapearPorDocente(Long direccionId) {
        Map<Long, List<NivelAcademico>> porDocente = new LinkedHashMap<>();
        for (DocenteGuia asignacion : docenteGuiaRepository
                .findByDireccionIdOrderByNivel_GradoAscNivel_SeccionAsc(direccionId)) {
            porDocente.computeIfAbsent(asignacion.getDocente().getId(), id -> new ArrayList<>())
                    .add(asignacion.getNivel());
        }
        return porDocente;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reemplazar(Long direccionId, Long docenteId, boolean profesorGuia, List<Long> nivelIds) {
        if (!profesorGuia) {
            docenteGuiaRepository.deleteByDireccionIdAndDocenteId(direccionId, docenteId);
            return;
        }
        if (nivelIds == null || nivelIds.isEmpty()) {
            throw new IllegalArgumentException("Indique de qué secciones es profesor guía");
        }

        Usuario docente = usuarioRepository.findById(docenteId)
                .filter(u -> u.getDirecciones().stream().anyMatch(i -> i.getId().equals(direccionId)))
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));

        Set<Long> ids = new LinkedHashSet<>(nivelIds);
        for (Long nivelId : ids) {
            NivelAcademico nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
            docenteGuiaRepository.findByDireccionIdAndNivelId(direccionId, nivelId)
                    .filter(existente -> !existente.getDocente().getId().equals(docenteId))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException(
                                "La sección " + nivel.getNombreCompleto()
                                        + " ya tiene profesor guía: " + existente.getDocente().getNombre());
                    });
        }

        docenteGuiaRepository.deleteByDireccionIdAndDocenteId(direccionId, docenteId);
        docenteGuiaRepository.flush();

        for (Long nivelId : ids) {
            NivelAcademico nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
            DocenteGuia asignacion = new DocenteGuia();
            asignacion.setDireccion(direccion);
            asignacion.setDocente(docente);
            asignacion.setNivel(nivel);
            docenteGuiaRepository.save(asignacion);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorDocente(Long docenteId) {
        docenteGuiaRepository.deleteByDocenteId(docenteId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorNivel(Long direccionId, Long nivelId) {
        docenteGuiaRepository.deleteByDireccionIdAndNivelId(direccionId, nivelId);
    }
}
