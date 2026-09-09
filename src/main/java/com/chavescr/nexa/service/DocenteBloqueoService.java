package com.chavescr.nexa.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DocenteBloqueoLeccion;
import com.chavescr.nexa.repository.DocenteBloqueoLeccionRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class DocenteBloqueoService {

    private final DocenteBloqueoLeccionRepository bloqueoRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final InstitucionRepository institucionRepository;

    public DocenteBloqueoService(DocenteBloqueoLeccionRepository bloqueoRepository,
            HorarioLeccionRepository horarioRepository,
            UsuarioRepository usuarioRepository,
            PeriodoAcademicoRepository periodoRepository,
            InstitucionRepository institucionRepository) {
        this.bloqueoRepository = bloqueoRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.periodoRepository = periodoRepository;
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<String> claves(Long institucionId, Long periodoId, Long docenteId) {
        if (periodoId == null || docenteId == null) {
            return Set.of();
        }
        return bloqueoRepository.findByInstitucionIdAndPeriodoIdAndDocenteId(institucionId, periodoId, docenteId)
                .stream()
                .map(b -> ConfiguracionAcademicaService.clave(b.getDia(), b.getNumeroLeccion()))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<Long> docenteIds(Long institucionId, Long periodoId, String dia, Integer numeroLeccion) {
        return new HashSet<>(bloqueoRepository.findDocenteIdsBloqueados(
                institucionId, periodoId, dia, numeroLeccion));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaBloqueado(Long institucionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion) {
        return bloqueoRepository.existsByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                institucionId, periodoId, docenteId, dia, numeroLeccion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void alternar(Long institucionId, Long docenteId, Long periodoId, String dia, Integer numeroLeccion) {
        if (!ConfiguracionAcademicaService.DIAS.contains(dia)
                || !ConfiguracionAcademicaService.LECCIONES.contains(numeroLeccion)) {
            throw new IllegalArgumentException("Día o número de lección inválido");
        }
        if (!horarioRepository
                .findByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                        institucionId, periodoId, docenteId, dia, numeroLeccion)
                .isEmpty()) {
            throw new IllegalArgumentException("No se puede bloquear una lección ya asignada");
        }
        bloqueoRepository
                .findByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                        institucionId, periodoId, docenteId, dia, numeroLeccion)
                .ifPresentOrElse(bloqueoRepository::delete, () -> {
                    DocenteBloqueoLeccion bloqueo = new DocenteBloqueoLeccion();
                    bloqueo.setInstitucion(institucionRepository.findById(institucionId)
                            .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada")));
                    bloqueo.setDocente(usuarioRepository.findActivoByIdAndInstitucionId(docenteId, institucionId)
                            .orElseThrow(() -> new IllegalArgumentException("Docente no válido")));
                    bloqueo.setPeriodo(periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
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
    public void eliminarPorPeriodo(Long institucionId, Long periodoId) {
        bloqueoRepository.deleteByInstitucionIdAndPeriodoId(institucionId, periodoId);
    }
}
