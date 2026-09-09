package com.chavescr.nexa.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Peso efectivo de cada indicador dentro del cotidiano (base 100).
     * Los de porcentaje fijo conservan su valor; el resto (100 − suma de fijos) se reparte
     * equitativamente entre los ponderados (porcentaje null).
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> calcularPesosEfectivos(List<IndicadorCotidiano> indicadores) {
        Map<Long, Double> pesos = new LinkedHashMap<>();
        if (indicadores == null || indicadores.isEmpty()) {
            return pesos;
        }
        int sumaFijos = indicadores.stream()
                .filter(i -> i.getPorcentaje() != null)
                .mapToInt(IndicadorCotidiano::getPorcentaje)
                .sum();
        long ponderados = indicadores.stream().filter(IndicadorCotidiano::isPonderado).count();
        double resto = Math.max(0, 100 - sumaFijos);
        double pesoPonderado = ponderados == 0 ? 0 : resto / ponderados;
        for (IndicadorCotidiano indicador : indicadores) {
            pesos.put(indicador.getId(),
                    indicador.isPonderado() ? pesoPonderado : indicador.getPorcentaje().doubleValue());
        }
        return pesos;
    }

    public IndicadorCotidiano guardarIndicador(Long institucionId, Long nivelId, Long materiaId,
            IndicadorCotidiano datos) {
        NivelAcademico nivel = nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        Integer puntosTotales = datos.getPuntosTotales();
        if (puntosTotales == null || puntosTotales < 1) {
            throw new IllegalArgumentException("Debes indicar los puntos totales del indicador");
        }

        List<IndicadorCotidiano> existentes = listarIndicadores(institucionId, nivelId, materiaId);
        int sumaFijosOtros = existentes.stream()
                .filter(i -> datos.getId() == null || !i.getId().equals(datos.getId()))
                .filter(i -> i.getPorcentaje() != null)
                .mapToInt(IndicadorCotidiano::getPorcentaje)
                .sum();
        long ponderadosOtros = existentes.stream()
                .filter(i -> datos.getId() == null || !i.getId().equals(datos.getId()))
                .filter(IndicadorCotidiano::isPonderado)
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
            if (ponderadosOtros > 0 && sumaFijosOtros + porcentaje >= 100) {
                throw new IllegalArgumentException(
                        "Debes dejar un porcentaje disponible para los indicadores ponderados (máximo: "
                                + Math.max(0, 99 - sumaFijosOtros) + "%)");
            }
        } else if (sumaFijosOtros >= 100) {
            throw new IllegalArgumentException(
                    "El 100% ya está asignado a indicadores con porcentaje fijo. Reduce uno de ellos para poder ponderar este.");
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
