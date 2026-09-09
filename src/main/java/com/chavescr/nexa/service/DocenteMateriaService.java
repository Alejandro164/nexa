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
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DocenteMateriaRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class DocenteMateriaService {

    private final DocenteMateriaRepository docenteMateriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final InstitucionRepository institucionRepository;

    public DocenteMateriaService(DocenteMateriaRepository docenteMateriaRepository,
            UsuarioRepository usuarioRepository,
            MateriaRepository materiaRepository,
            InstitucionRepository institucionRepository) {
        this.docenteMateriaRepository = docenteMateriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.materiaRepository = materiaRepository;
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Materia> listarMaterias(Long institucionId, Long docenteId) {
        return docenteMateriaRepository
                .findByInstitucionIdAndDocenteIdOrderByMateria_NombreAsc(institucionId, docenteId)
                .stream()
                .map(DocenteMateria::getMateria)
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Long> listarMateriaIds(Long institucionId, Long docenteId) {
        return listarMaterias(institucionId, docenteId).stream().map(Materia::getId).toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> listarDocentesPorMateria(Long institucionId, Long materiaId) {
        if (materiaId == null) {
            return List.of();
        }
        Map<Long, Usuario> porId = new LinkedHashMap<>();
        for (DocenteMateria asignacion : docenteMateriaRepository
                .findByInstitucionIdAndMateriaIdOrderByDocente_NombreAsc(institucionId, materiaId)) {
            Usuario docente = asignacion.getDocente();
            if (Boolean.TRUE.equals(docente.getActivo())) {
                porId.putIfAbsent(docente.getId(), docente);
            }
        }
        return new ArrayList<>(porId.values());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaAsignado(Long institucionId, Long docenteId, Long materiaId) {
        return docenteId != null && materiaId != null
                && docenteMateriaRepository.existsByInstitucionIdAndDocenteIdAndMateriaId(
                        institucionId, docenteId, materiaId);
    }

    /**
     * Materias activas de la institución, más las ya asignadas aunque estén inactivas,
     * para que no desaparezcan del formulario al desactivarlas.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Materia> catalogoParaFormulario(Long institucionId, Long docenteId) {
        List<Materia> activas = materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
        if (docenteId == null) {
            return activas;
        }
        Map<Long, Materia> porId = new LinkedHashMap<>();
        for (Materia materia : activas) {
            porId.put(materia.getId(), materia);
        }
        for (Materia materia : listarMaterias(institucionId, docenteId)) {
            porId.putIfAbsent(materia.getId(), materia);
        }
        return new ArrayList<>(porId.values());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, List<Materia>> mapearPorDocente(Long institucionId) {
        Map<Long, List<Materia>> porDocente = new LinkedHashMap<>();
        for (DocenteMateria asignacion : docenteMateriaRepository
                .findByInstitucionIdOrderByMateria_NombreAsc(institucionId)) {
            porDocente.computeIfAbsent(asignacion.getDocente().getId(), id -> new ArrayList<>())
                    .add(asignacion.getMateria());
        }
        return porDocente;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reemplazar(Long institucionId, Long docenteId, List<Long> materiaIds) {
        Usuario docente = usuarioRepository.findById(docenteId)
                .filter(u -> u.getInstituciones().stream().anyMatch(i -> i.getId().equals(institucionId)))
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        docenteMateriaRepository.deleteByInstitucionIdAndDocenteId(institucionId, docenteId);
        docenteMateriaRepository.flush();

        if (materiaIds == null || materiaIds.isEmpty()) {
            return;
        }

        Set<Long> ids = new LinkedHashSet<>(materiaIds);
        for (Long materiaId : ids) {
            Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                    .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
            DocenteMateria asignacion = new DocenteMateria();
            asignacion.setInstitucion(institucion);
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
    public void eliminarPorMateria(Long institucionId, Long materiaId) {
        docenteMateriaRepository.deleteByInstitucionIdAndMateriaId(institucionId, materiaId);
    }
}
