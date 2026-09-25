package com.chavescr.nexa.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.SeccionBloqueoLeccion;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
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
    private final DireccionRepository direccionRepository;
    private final TipoMateriaRepository tipoMateriaRepository;
    private final ConfiguracionDireccionService configuracionDireccionService;

    public SeccionBloqueoService(SeccionBloqueoLeccionRepository bloqueoRepository,
            HorarioLeccionRepository horarioRepository,
            NivelAcademicoRepository nivelRepository,
            PeriodoAcademicoRepository periodoRepository,
            DireccionRepository direccionRepository,
            TipoMateriaRepository tipoMateriaRepository,
            ConfiguracionDireccionService configuracionDireccionService) {
        this.bloqueoRepository = bloqueoRepository;
        this.horarioRepository = horarioRepository;
        this.nivelRepository = nivelRepository;
        this.periodoRepository = periodoRepository;
        this.direccionRepository = direccionRepository;
        this.tipoMateriaRepository = tipoMateriaRepository;
        this.configuracionDireccionService = configuracionDireccionService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<String, TipoMateria> mapa(Long direccionId, Long periodoId, Long nivelId) {
        Map<String, TipoMateria> mapa = new LinkedHashMap<>();
        if (periodoId == null || nivelId == null) {
            return mapa;
        }
        bloqueoRepository.findByDireccionIdAndPeriodoIdAndNivelId(direccionId, periodoId, nivelId)
                .forEach(b -> mapa.put(ConfiguracionAcademicaService.clave(b.getDia(), b.getNumeroLeccion()),
                        b.getTipoMateria()));
        return mapa;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<TipoMateria> tipoBloqueado(Long direccionId, Long periodoId, Long nivelId, String dia,
            Integer numeroLeccion) {
        if (periodoId == null || nivelId == null) {
            return Optional.empty();
        }
        return bloqueoRepository
                .findByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
                        direccionId, periodoId, nivelId, dia, numeroLeccion)
                .map(SeccionBloqueoLeccion::getTipoMateria);
    }

    @Transactional(rollbackFor = Exception.class)
    public void alternar(Long direccionId, Long periodoId, Long nivelId, String dia, Integer numeroLeccion,
            Long tipoMateriaId) {
        var config = configuracionDireccionService.obtener(direccionId);
        if (!config.getDias().contains(dia) || !config.getLecciones().contains(numeroLeccion)) {
            throw new IllegalArgumentException("Día o número de lección inválido");
        }
        if (horarioRepository.existsByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
                direccionId, periodoId, nivelId, dia, numeroLeccion)) {
            throw new IllegalArgumentException("No se puede bloquear una lección ya asignada");
        }

        Optional<SeccionBloqueoLeccion> existente = bloqueoRepository
                .findByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccion(
                        direccionId, periodoId, nivelId, dia, numeroLeccion);
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
        bloqueo.setDireccion(direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada")));
        bloqueo.setNivel(nivelRepository.findByIdAndDireccionId(nivelId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado")));
        bloqueo.setPeriodo(periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado")));
        bloqueo.setTipoMateria(tipoMateriaRepository.findById(tipoMateriaId)
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de materia no encontrado")));
        bloqueo.setDia(dia);
        bloqueo.setNumeroLeccion(numeroLeccion);
        bloqueoRepository.save(bloqueo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorNivel(Long direccionId, Long nivelId) {
        bloqueoRepository.deleteByDireccionIdAndNivelId(direccionId, nivelId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorPeriodo(Long direccionId, Long periodoId) {
        bloqueoRepository.deleteByDireccionIdAndPeriodoId(direccionId, periodoId);
    }
}
