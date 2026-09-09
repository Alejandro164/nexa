package com.chavescr.nexa.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.TareaDefinicion;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.TareaCalificacionRepository;
import com.chavescr.nexa.repository.TareaDefinicionRepository;

@Service
@Transactional
public class TareaDefinicionService {

    private final TareaDefinicionRepository tareaDefinicionRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final TareaCalificacionRepository calificacionRepository;

    public TareaDefinicionService(TareaDefinicionRepository tareaDefinicionRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository,
            TareaCalificacionRepository calificacionRepository) {
        this.tareaDefinicionRepository = tareaDefinicionRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.calificacionRepository = calificacionRepository;
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

    /**
     * Peso efectivo de cada tarea dentro del componente (base 100).
     * Las de porcentaje fijo conservan su valor; el resto (100 − suma de fijos) se reparte
     * equitativamente entre las ponderadas (porcentaje null).
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPesosEfectivos(List<TareaDefinicion> tareas) {
        Map<Long, Double> pesos = new LinkedHashMap<>();
        if (tareas == null || tareas.isEmpty()) {
            return pesos;
        }
        int sumaFijos = tareas.stream()
                .filter(t -> t.getPorcentaje() != null)
                .mapToInt(TareaDefinicion::getPorcentaje)
                .sum();
        long ponderadas = tareas.stream().filter(TareaDefinicion::isPonderado).count();
        double resto = Math.max(0, 100 - sumaFijos);
        double pesoPonderado = ponderadas == 0 ? 0 : resto / ponderadas;
        for (TareaDefinicion tarea : tareas) {
            pesos.put(tarea.getId(),
                    tarea.isPonderado() ? pesoPonderado : tarea.getPorcentaje().doubleValue());
        }
        return pesos;
    }

    public TareaDefinicion guardarTarea(Long institucionId, Long nivelId, Long materiaId, TareaDefinicion datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        Integer puntosTotales = datos.getPuntosTotales();
        if (puntosTotales == null || puntosTotales < 1) {
            throw new IllegalArgumentException("Debes indicar los puntos totales de la tarea");
        }

        List<TareaDefinicion> existentes = listarTareas(institucionId, nivelId, materiaId);
        int sumaFijosOtros = existentes.stream()
                .filter(t -> datos.getId() == null || !t.getId().equals(datos.getId()))
                .filter(t -> t.getPorcentaje() != null)
                .mapToInt(TareaDefinicion::getPorcentaje)
                .sum();
        long ponderadasOtras = existentes.stream()
                .filter(t -> datos.getId() == null || !t.getId().equals(datos.getId()))
                .filter(TareaDefinicion::isPonderado)
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
                        "Debes dejar un porcentaje disponible para las tareas ponderadas (máximo: "
                                + Math.max(0, 99 - sumaFijosOtros) + "%)");
            }
        } else if (sumaFijosOtros >= 100) {
            throw new IllegalArgumentException(
                    "El 100% ya está asignado a tareas con porcentaje fijo. Reduce una de ellas para poder ponderar esta.");
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
        tarea.setPuntosTotales(puntosTotales);
        return tareaDefinicionRepository.save(tarea);
    }

    public void eliminarTarea(Long institucionId, Long id) {
        TareaDefinicion tarea = obtenerTarea(institucionId, id);
        if (calificacionRepository.existsByTareaDefinicionId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: la tarea ya tiene calificaciones registradas");
        }
        tareaDefinicionRepository.delete(tarea);
    }
}
