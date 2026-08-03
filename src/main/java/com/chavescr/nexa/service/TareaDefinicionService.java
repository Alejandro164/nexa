package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.TareaDefinicion;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.TareaDefinicionRepository;

@Service
@Transactional
public class TareaDefinicionService {

    private final TareaDefinicionRepository tareaDefinicionRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;

    public TareaDefinicionService(TareaDefinicionRepository tareaDefinicionRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository) {
        this.tareaDefinicionRepository = tareaDefinicionRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarNivelesActivos(Long institucionId) {
        return nivelRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<TareaDefinicion> listarTareas(Long institucionId, Long nivelId, Long materiaId) {
        return tareaDefinicionRepository.findByInstitucionIdAndNivelIdAndMateriaIdOrderByFechaEntregaAsc(
                institucionId, nivelId, materiaId);
    }

    @Transactional(readOnly = true)
    public TareaDefinicion obtenerTarea(Long institucionId, Long id) {
        return tareaDefinicionRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    public TareaDefinicion guardarTarea(Long institucionId, Long nivelId, Long materiaId, TareaDefinicion datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        int porcentaje = datos.getPorcentaje() != null ? datos.getPorcentaje() : 0;
        int sumaExistente = listarTareas(institucionId, nivelId, materiaId).stream()
                .filter(t -> !t.getId().equals(datos.getId()))
                .mapToInt(TareaDefinicion::getPorcentaje)
                .sum();
        if (sumaExistente + porcentaje > 100) {
            throw new IllegalArgumentException(
                    "La suma de las tareas no puede superar 100% (actual: " + sumaExistente + "%)");
        }

        TareaDefinicion tarea = datos.getId() != null
                ? obtenerTarea(institucionId, datos.getId())
                : new TareaDefinicion();
        tarea.setInstitucion(nivel.getInstitucion());
        tarea.setNivel(nivel);
        tarea.setMateria(materia);
        tarea.setTitulo(datos.getTitulo().trim());
        tarea.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        tarea.setFechaEntrega(datos.getFechaEntrega());
        tarea.setPorcentaje(porcentaje);
        return tareaDefinicionRepository.save(tarea);
    }

    public void eliminarTarea(Long institucionId, Long id) {
        TareaDefinicion tarea = obtenerTarea(institucionId, id);
        tareaDefinicionRepository.delete(tarea);
    }
}
