package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.repository.EvaluacionCotidianaRepository;
import com.chavescr.nexa.repository.IndicadorCotidianoRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;

@Service
@Transactional
public class IndicadorCotidianoService {

    private final IndicadorCotidianoRepository indicadorRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final EvaluacionCotidianaRepository evaluacionRepository;

    public IndicadorCotidianoService(IndicadorCotidianoRepository indicadorRepository,
            NivelAcademicoRepository nivelRepository, MateriaRepository materiaRepository,
            EvaluacionCotidianaRepository evaluacionRepository) {
        this.indicadorRepository = indicadorRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.evaluacionRepository = evaluacionRepository;
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
    public List<IndicadorCotidiano> listarIndicadores(Long institucionId, Long nivelId, Long materiaId) {
        return indicadorRepository.findByInstitucionIdAndNivelIdAndMateriaIdOrderByIdAsc(
                institucionId, nivelId, materiaId);
    }

    @Transactional(readOnly = true)
    public IndicadorCotidiano obtenerIndicador(Long institucionId, Long id) {
        return indicadorRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Indicador no encontrado"));
    }

    public IndicadorCotidiano guardarIndicador(Long institucionId, Long nivelId, Long materiaId,
            IndicadorCotidiano datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        int porcentaje = datos.getPorcentaje() != null ? datos.getPorcentaje() : 0;
        int sumaExistente = listarIndicadores(institucionId, nivelId, materiaId).stream()
                .filter(i -> !i.getId().equals(datos.getId()))
                .mapToInt(IndicadorCotidiano::getPorcentaje)
                .sum();
        if (sumaExistente + porcentaje > 100) {
            throw new IllegalArgumentException(
                    "La suma de los indicadores no puede superar 100% (actual: " + sumaExistente + "%)");
        }

        Integer puntosTotales = datos.getPuntosTotales();
        if (puntosTotales != null && puntosTotales < 0) {
            throw new IllegalArgumentException("Los puntos totales no pueden ser negativos");
        }

        IndicadorCotidiano indicador = datos.getId() != null
                ? obtenerIndicador(institucionId, datos.getId())
                : new IndicadorCotidiano();
        indicador.setInstitucion(nivel.getInstitucion());
        indicador.setNivel(nivel);
        indicador.setMateria(materia);
        indicador.setTitulo(datos.getTitulo().trim());
        indicador.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        indicador.setPorcentaje(porcentaje);
        indicador.setPuntosTotales(puntosTotales);
        return indicadorRepository.save(indicador);
    }

    public void eliminarIndicador(Long institucionId, Long id) {
        IndicadorCotidiano indicador = obtenerIndicador(institucionId, id);
        if (evaluacionRepository.existsByIndicadorId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el indicador ya tiene calificaciones registradas");
        }
        indicadorRepository.delete(indicador);
    }
}
