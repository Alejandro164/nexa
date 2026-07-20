package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.repository.IndicadorCotidianoRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;

@Service
@Transactional
public class IndicadorCotidianoService {

    private final IndicadorCotidianoRepository indicadorRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final MateriaRepository materiaRepository;

    public IndicadorCotidianoService(IndicadorCotidianoRepository indicadorRepository,
            PeriodoAcademicoRepository periodoRepository, MateriaRepository materiaRepository) {
        this.indicadorRepository = indicadorRepository;
        this.periodoRepository = periodoRepository;
        this.materiaRepository = materiaRepository;
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodosActivos(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<IndicadorCotidiano> listarIndicadores(Long institucionId, Long periodoId, Long materiaId) {
        return indicadorRepository.findByInstitucionIdAndPeriodoIdAndMateriaIdOrderByIdAsc(
                institucionId, periodoId, materiaId);
    }

    @Transactional(readOnly = true)
    public IndicadorCotidiano obtenerIndicador(Long institucionId, Long id) {
        return indicadorRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Indicador no encontrado"));
    }

    public IndicadorCotidiano guardarIndicador(Long institucionId, Long periodoId, Long materiaId,
            IndicadorCotidiano datos) {
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Materia materia = materiaRepository.findByIdAndInstitucionId(materiaId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));

        int porcentaje = datos.getPorcentaje() != null ? datos.getPorcentaje() : 0;
        int sumaExistente = listarIndicadores(institucionId, periodoId, materiaId).stream()
                .filter(i -> !i.getId().equals(datos.getId()))
                .mapToInt(IndicadorCotidiano::getPorcentaje)
                .sum();
        if (sumaExistente + porcentaje > 100) {
            throw new IllegalArgumentException(
                    "La suma de los indicadores no puede superar 100% (actual: " + sumaExistente + "%)");
        }

        IndicadorCotidiano indicador = datos.getId() != null
                ? obtenerIndicador(institucionId, datos.getId())
                : new IndicadorCotidiano();
        indicador.setInstitucion(periodo.getInstitucion());
        indicador.setPeriodo(periodo);
        indicador.setMateria(materia);
        indicador.setTitulo(datos.getTitulo().trim());
        indicador.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        indicador.setPorcentaje(porcentaje);
        return indicadorRepository.save(indicador);
    }

    public void eliminarIndicador(Long institucionId, Long id) {
        IndicadorCotidiano indicador = obtenerIndicador(institucionId, id);
        indicadorRepository.delete(indicador);
    }
}
