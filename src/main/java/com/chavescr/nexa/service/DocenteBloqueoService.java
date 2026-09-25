package com.chavescr.nexa.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DocenteBloqueoLeccion;
import com.chavescr.nexa.repository.DocenteBloqueoLeccionRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class DocenteBloqueoService {

    private final DocenteBloqueoLeccionRepository bloqueoRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final DireccionRepository direccionRepository;
    private final ConfiguracionDireccionService configuracionDireccionService;

    public DocenteBloqueoService(DocenteBloqueoLeccionRepository bloqueoRepository,
            HorarioLeccionRepository horarioRepository,
            UsuarioRepository usuarioRepository,
            PeriodoAcademicoRepository periodoRepository,
            DireccionRepository direccionRepository,
            ConfiguracionDireccionService configuracionDireccionService) {
        this.bloqueoRepository = bloqueoRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.periodoRepository = periodoRepository;
        this.direccionRepository = direccionRepository;
        this.configuracionDireccionService = configuracionDireccionService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<String> claves(Long direccionId, Long periodoId, Long docenteId) {
        if (periodoId == null || docenteId == null) {
            return Set.of();
        }
        return bloqueoRepository.findByDireccionIdAndPeriodoIdAndDocenteId(direccionId, periodoId, docenteId)
                .stream()
                .map(b -> ConfiguracionAcademicaService.clave(b.getDia(), b.getNumeroLeccion()))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<Long> docenteIds(Long direccionId, Long periodoId, String dia, Integer numeroLeccion) {
        return new HashSet<>(bloqueoRepository.findDocenteIdsBloqueados(
                direccionId, periodoId, dia, numeroLeccion));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaBloqueado(Long direccionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion) {
        return bloqueoRepository.existsByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                direccionId, periodoId, docenteId, dia, numeroLeccion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void alternar(Long direccionId, Long docenteId, Long periodoId, String dia, Integer numeroLeccion) {
        var config = configuracionDireccionService.obtener(direccionId);
        if (!config.getDias().contains(dia) || !config.getLecciones().contains(numeroLeccion)) {
            throw new IllegalArgumentException("Día o número de lección inválido");
        }
        if (!horarioRepository
                .findByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                        direccionId, periodoId, docenteId, dia, numeroLeccion)
                .isEmpty()) {
            throw new IllegalArgumentException("No se puede bloquear una lección ya asignada");
        }
        bloqueoRepository
                .findByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                        direccionId, periodoId, docenteId, dia, numeroLeccion)
                .ifPresentOrElse(bloqueoRepository::delete, () -> {
                    DocenteBloqueoLeccion bloqueo = new DocenteBloqueoLeccion();
                    bloqueo.setDireccion(direccionRepository.findById(direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada")));
                    bloqueo.setDocente(usuarioRepository.findActivoByIdAndDireccionId(docenteId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Docente no válido")));
                    bloqueo.setPeriodo(periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Período no encontrado")));
                    bloqueo.setDia(dia);
                    bloqueo.setNumeroLeccion(numeroLeccion);
                    bloqueoRepository.save(bloqueo);
                });
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorDocente(Long docenteId) {
        bloqueoRepository.deleteByDocenteId(docenteId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorPeriodo(Long direccionId, Long periodoId) {
        bloqueoRepository.deleteByDireccionIdAndPeriodoId(direccionId, periodoId);
    }
}
