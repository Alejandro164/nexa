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
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DocenteGuiaRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class DocenteGuiaService {

    private final DocenteGuiaRepository docenteGuiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final InstitucionRepository institucionRepository;

    public DocenteGuiaService(DocenteGuiaRepository docenteGuiaRepository,
            UsuarioRepository usuarioRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            InstitucionRepository institucionRepository) {
        this.docenteGuiaRepository = docenteGuiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<NivelAcademico> listarSecciones(Long institucionId, Long docenteId) {
        return docenteGuiaRepository
                .findByInstitucionIdAndDocenteIdOrderByNivel_GradoAscNivel_SeccionAsc(institucionId, docenteId)
                .stream()
                .map(DocenteGuia::getNivel)
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Long> listarNivelIds(Long institucionId, Long docenteId) {
        return listarSecciones(institucionId, docenteId).stream().map(NivelAcademico::getId).toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean esProfesorGuia(Long institucionId, Long docenteId) {
        return !listarNivelIds(institucionId, docenteId).isEmpty();
    }

    /**
     * Secciones activas, más las ya asignadas aunque estén inactivas.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<NivelAcademico> catalogoParaFormulario(Long institucionId, Long docenteId) {
        List<NivelAcademico> activas = nivelAcademicoRepository
                .findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
        if (docenteId == null) {
            return activas;
        }
        Map<Long, NivelAcademico> porId = new LinkedHashMap<>();
        for (NivelAcademico nivel : activas) {
            porId.put(nivel.getId(), nivel);
        }
        for (NivelAcademico nivel : listarSecciones(institucionId, docenteId)) {
            porId.putIfAbsent(nivel.getId(), nivel);
        }
        return new ArrayList<>(porId.values());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, List<NivelAcademico>> mapearPorDocente(Long institucionId) {
        Map<Long, List<NivelAcademico>> porDocente = new LinkedHashMap<>();
        for (DocenteGuia asignacion : docenteGuiaRepository
                .findByInstitucionIdOrderByNivel_GradoAscNivel_SeccionAsc(institucionId)) {
            porDocente.computeIfAbsent(asignacion.getDocente().getId(), id -> new ArrayList<>())
                    .add(asignacion.getNivel());
        }
        return porDocente;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reemplazar(Long institucionId, Long docenteId, boolean profesorGuia, List<Long> nivelIds) {
        if (!profesorGuia) {
            docenteGuiaRepository.deleteByInstitucionIdAndDocenteId(institucionId, docenteId);
            return;
        }
        if (nivelIds == null || nivelIds.isEmpty()) {
            throw new IllegalArgumentException("Indique de qué secciones es profesor guía");
        }

        Usuario docente = usuarioRepository.findById(docenteId)
                .filter(u -> u.getInstituciones().stream().anyMatch(i -> i.getId().equals(institucionId)))
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        Set<Long> ids = new LinkedHashSet<>(nivelIds);
        for (Long nivelId : ids) {
            NivelAcademico nivel = nivelAcademicoRepository.findByIdAndInstitucionId(nivelId, institucionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
            docenteGuiaRepository.findByInstitucionIdAndNivelId(institucionId, nivelId)
                    .filter(existente -> !existente.getDocente().getId().equals(docenteId))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException(
                                "La sección " + nivel.getNombreCompleto()
                                        + " ya tiene profesor guía: " + existente.getDocente().getNombre());
                    });
        }

        docenteGuiaRepository.deleteByInstitucionIdAndDocenteId(institucionId, docenteId);
        docenteGuiaRepository.flush();

        for (Long nivelId : ids) {
            NivelAcademico nivel = nivelAcademicoRepository.findByIdAndInstitucionId(nivelId, institucionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
            DocenteGuia asignacion = new DocenteGuia();
            asignacion.setInstitucion(institucion);
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
    public void eliminarPorNivel(Long institucionId, Long nivelId) {
        docenteGuiaRepository.deleteByInstitucionIdAndNivelId(institucionId, nivelId);
    }
}
