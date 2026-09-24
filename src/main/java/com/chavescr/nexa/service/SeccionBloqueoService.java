package com.chavescr.nexa.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.SeccionBloqueoLeccion;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.SeccionBloqueoLeccionRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;

@Service
@Transactional
public class SeccionBloqueoService {

    private final SeccionBloqueoLeccionRepository bloqueoRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final InstitucionRepository institucionRepository;
    private final TipoMateriaRepository tipoMateriaRepository;
    private final ConfiguracionInstitucionService configuracionInstitucionService;

    public SeccionBloqueoService(SeccionBloqueoLeccionRepository bloqueoRepository,
            HorarioLeccionRepository horarioRepository,
            NivelAcademicoRepository nivelRepository,
            PeriodoAcademicoRepository periodoRepository,
            InstitucionRepository institucionRepository,
            TipoMateriaRepository tipoMateriaRepository,
            ConfiguracionInstitucionService configuracionInstitucionService) {
        this.bloqueoRepository = bloqueoRepository;
        this.horarioRepository = horarioRepository;
        this.nivelRepository = nivelRepository;
        this.periodoRepository = periodoRepository;
        this.institucionRepository = institucionRepository;
        this.tipoMateriaRepository = tipoMateriaRepository;
        this.configuracionInstitucionService = configuracionInstitucionService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<String, TipoMateria> mapa(Long institucionId, Long periodoId, Long nivelId) {
        Map<String, TipoMateria> mapa = new LinkedHashMap<>();
        if (periodoId == null || nivelId == null) {
            return mapa;
        }
        bloqueoRepository.findByInstitucionIdAndPeriodoIdAndNivelId(institucionId, periodoId, nivelId)
                .forEach(b -> mapa.put(ConfiguracionAcademicaService.clave(b.getDia(), b.getNumeroLeccion()),
                        b.getTipoMateria()));
        return mapa;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<TipoMateria> tipoBloqueado(Long institucionId, Long periodoId, Long nivelId, String dia,
            Integer numeroLeccion) {
        if (periodoId == null || nivelId == null) {
            return Optional.empty();
        }
        return bloqueoRepository
                .findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
                        institucionId, periodoId, nivelId, dia, numeroLeccion)
                .map(SeccionBloqueoLeccion::getTipoMateria);
    }

    @Transactional(rollbackFor = Exception.class)
    public void alternar(Long institucionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion,
            Long tipoMateriaId) {
        var config = configuracionInstitucionService.obtener(institucionId);
        if (!config.getDias().contains(dia) || !config.getLecciones().contains(numeroLeccion)) {
            throw new IllegalArgumentException("Día o número de lección inválido");
        }
        if (horarioRepository.existsByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
                institucionId, periodoId, nivelId, dia, numeroLeccion)) {
            throw new IllegalArgumentException("No se puede bloquear una lección ya asignada");
        }

        Optional<SeccionBloqueoLeccion> existente = bloqueoRepository
                .findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
                        institucionId, periodoId, nivelId, dia, numeroLeccion);
        if (existente.isPresent()) {
            SeccionBloqueoLeccion bloqueo = existente.get();
            if (!bloqueo.getTipoMateria().getId().equals(tipoMateriaId)) {
                throw new IllegalArgumentException(
                        "Esta lección ya está bloqueada para " + bloqueo.getTipoMateria().getNombre());
            }
            bloqueoRepository.delete(bloqueo);
            return;
        }

        SeccionBloqueoLeccion bloqueo = new SeccionBloqueoLeccion();
        bloqueo.setInstitucion(institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada")));
        bloqueo.setNivel(nivelRepository.findByIdAndInstitucionId(nivelId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado")));
        bloqueo.setPeriodo(periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado")));
        bloqueo.setTipoMateria(tipoMateriaRepository.findById(tipoMateriaId)
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de materia no encontrado")));
        bloqueo.setDia(dia);
        bloqueo.setNumeroLeccion(numeroLeccion);
        bloqueoRepository.save(bloqueo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorNivel(Long institucionId, Long nivelId) {
        bloqueoRepository.deleteByInstitucionIdAndNivelId(institucionId, nivelId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorPeriodo(Long institucionId, Long periodoId) {
        bloqueoRepository.deleteByInstitucionIdAndPeriodoId(institucionId, periodoId);
    }
}
