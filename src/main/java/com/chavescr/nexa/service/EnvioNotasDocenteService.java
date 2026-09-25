package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.EnvioNotasDocente;
import com.chavescr.nexa.repository.EnvioNotasDocenteRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class EnvioNotasDocenteService {

    public record DocentePendiente(Long docenteId, String nombre, int combosPendientes) {
    }

    private final EnvioNotasDocenteRepository envioRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final DireccionRepository direccionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final NivelAcademicoRepository nivelRepository;

    public EnvioNotasDocenteService(EnvioNotasDocenteRepository envioRepository,
            HorarioLeccionRepository horarioRepository,
            DireccionRepository direccionRepository,
            PeriodoAcademicoRepository periodoRepository,
            UsuarioRepository usuarioRepository,
            MateriaRepository materiaRepository,
            NivelAcademicoRepository nivelRepository) {
        this.envioRepository = envioRepository;
        this.horarioRepository = horarioRepository;
        this.direccionRepository = direccionRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
        this.materiaRepository = materiaRepository;
        this.nivelRepository = nivelRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean estaEnviado(Long direccionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId) {
        return envioRepository.existsByDireccionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
                direccionId, periodoId, docenteId, materiaId, nivelId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enviar(Long direccionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId) {
        envioRepository.findByDireccionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
                direccionId, periodoId, docenteId, materiaId, nivelId)
                .ifPresentOrElse(envio -> envio.setFechaEnvio(java.time.LocalDateTime.now()), () -> {
                    EnvioNotasDocente envio = new EnvioNotasDocente();
                    envio.setDireccion(direccionRepository.findById(direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada")));
                    envio.setPeriodo(periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Período no encontrado")));
                    envio.setDocente(usuarioRepository.findActivoByIdAndDireccionId(docenteId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Docente no válido")));
                    envio.setMateria(materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada")));
                    envio.setNivel(nivelRepository.findByIdAndDireccionId(nivelId, direccionId)
                            .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado")));
                    envioRepository.save(envio);
                });
    }

    @Transactional(rollbackFor = Exception.class)
    public void deshacer(Long direccionId, Long periodoId, Long docenteId, Long materiaId, Long nivelId) {
        envioRepository.findByDireccionIdAndPeriodoIdAndDocenteIdAndMateriaIdAndNivelId(
                direccionId, periodoId, docenteId, materiaId, nivelId)
                .ifPresent(envioRepository::delete);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarPorPeriodo(Long direccionId, Long periodoId) {
        envioRepository.deleteByDireccionIdAndPeriodoId(direccionId, periodoId);
    }

    /** Docentes con al menos un combo materia+nivel del horario sin notas enviadas en ese período. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<DocentePendiente> listarPendientes(Long direccionId, Long periodoId) {
        Map<Long, Integer> pendientesPorDocente = new HashMap<>();
        for (Object[] fila : horarioRepository.findCombosDocenteMateriaNivel(direccionId, periodoId)) {
            Long docenteId = (Long) fila[0];
            Long materiaId = (Long) fila[1];
            Long nivelId = (Long) fila[2];
            if (!estaEnviado(direccionId, periodoId, docenteId, materiaId, nivelId)) {
                pendientesPorDocente.merge(docenteId, 1, Integer::sum);
            }
        }
        List<DocentePendiente> resultado = new ArrayList<>();
        pendientesPorDocente.forEach((docenteId, cantidad) -> usuarioRepository
                .findActivoByIdAndDireccionId(docenteId, direccionId)
                .ifPresent(docente -> resultado.add(new DocentePendiente(docenteId, docente.getNombre(), cantidad))));
        return resultado;
    }
}
