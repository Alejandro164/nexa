package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DocenteMateria;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DocenteMateriaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class DocenteMateriaService {

    private final DocenteMateriaRepository docenteMateriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final DireccionRepository direccionRepository;

    public DocenteMateriaService(DocenteMateriaRepository docenteMateriaRepository,
            UsuarioRepository usuarioRepository,
            MateriaRepository materiaRepository,
            DireccionRepository direccionRepository) {
        this.docenteMateriaRepository = docenteMateriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.materiaRepository = materiaRepository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Materia> listarMaterias(Long direccionId, Long docenteId) {
        return docenteMateriaRepository
                .findByDireccionIdAndDocenteIdOrderByMateria_NombreAsc(direccionId, docenteId)
                .stream()
                .map(DocenteMateria::getMateria)
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Long> listarMateriaIds(Long direccionId, Long docenteId) {
        return listarMaterias(direccionId, docenteId).stream().map(Materia::getId).toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> listarDocentesPorMateria(Long direccionId, Long materiaId) {
        if (materiaId == null) {
            return List.of();
        }
        Map<Long, Usuario> porId = new LinkedHashMap<>();
        for (DocenteMateria asignacion : docenteMateriaRepository
                .findByDireccionIdAndMateriaIdOrderByDocente_NombreAsc(direccionId, materiaId)) {
            Usuario docente = asignacion.getDocente();
            if (Boolean.TRUE.equals(docente.getActivo())) {
                porId.putIfAbsent(docente.getId(), docente);
            }
        }
        return new ArrayList<>(porId.values());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaAsignado(Long direccionId, Long docenteId, Long materiaId) {
        return docenteId != null && materiaId != null
                && docenteMateriaRepository.existsByDireccionIdAndDocenteIdAndMateriaId(
                        direccionId, docenteId, materiaId);
    }

    /**
     * Materias activas de la dirección, más las ya asignadas aunque estén inactivas,
     * para que no desaparezcan del formulario al desactivarlas.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Materia> catalogoParaFormulario(Long direccionId, Long docenteId) {
        List<Materia> activas = materiaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
        if (docenteId == null) {
            return activas;
        }
        Map<Long, Materia> porId = new LinkedHashMap<>();
        for (Materia materia : activas) {
            porId.put(materia.getId(), materia);
        }
        for (Materia materia : listarMaterias(direccionId, docenteId)) {
            porId.putIfAbsent(materia.getId(), materia);
        }
        return new ArrayList<>(porId.values());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, List<Materia>> mapearPorDocente(Long direccionId) {
        Map<Long, List<Materia>> porDocente = new LinkedHashMap<>();
        for (DocenteMateria asignacion : docenteMateriaRepository
                .findByDireccionIdOrderByMateria_NombreAsc(direccionId)) {
            porDocente.computeIfAbsent(asignacion.getDocente().getId(), id -> new ArrayList<>())
                    .add(asignacion.getMateria());
        }
        return porDocente;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reemplazar(Long direccionId, Long docenteId, List<Long> materiaIds) {
        Usuario docente = usuarioRepository.findById(docenteId)
                .filter(u -> u.getDirecciones().stream().anyMatch(i -> i.getId().equals(direccionId)))
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));

        docenteMateriaRepository.deleteByDireccionIdAndDocenteId(direccionId, docenteId);
        docenteMateriaRepository.flush();

        if (materiaIds == null || materiaIds.isEmpty()) {
            return;
        }

        Set<Long> ids = new LinkedHashSet<>(materiaIds);
        for (Long materiaId : ids) {
            Materia materia = materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
            DocenteMateria asignacion = new DocenteMateria();
            asignacion.setDireccion(direccion);
            asignacion.setDocente(docente);
            asignacion.setMateria(materia);
            docenteMateriaRepository.save(asignacion);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorDocente(Long docenteId) {
        docenteMateriaRepository.deleteByDocenteId(docenteId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorMateria(Long direccionId, Long materiaId) {
        docenteMateriaRepository.deleteByDireccionIdAndMateriaId(direccionId, materiaId);
    }
}
