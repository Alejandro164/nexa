package com.chavescr.nexa.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.ConfiguracionDireccion;
import com.chavescr.nexa.entity.DiaLaboral;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Jornada;
import com.chavescr.nexa.entity.Jornada.BloqueJornada;
import com.chavescr.nexa.entity.Jornada.TipoPausa;
import com.chavescr.nexa.repository.ConfiguracionDireccionRepository;
import com.chavescr.nexa.repository.DocenteBloqueoLeccionRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.DireccionRepository;

@Service
@Transactional
public class ConfiguracionDireccionService {

    private final ConfiguracionDireccionRepository configuracionRepository;
    private final DireccionRepository direccionRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final DocenteBloqueoLeccionRepository bloqueoRepository;

    public ConfiguracionDireccionService(ConfiguracionDireccionRepository configuracionRepository,
            DireccionRepository direccionRepository,
            HorarioLeccionRepository horarioRepository,
            DocenteBloqueoLeccionRepository bloqueoRepository) {
        this.configuracionRepository = configuracionRepository;
        this.direccionRepository = direccionRepository;
        this.horarioRepository = horarioRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ConfiguracionDireccion obtener(Long direccionId) {
        return configuracionRepository.findByDireccionId(direccionId)
                .orElseGet(() -> ConfiguracionDireccion.predeterminada(null));
    }

    @Transactional(rollbackFor = Exception.class)
    public ConfiguracionDireccion obtenerOCrear(Long direccionId) {
        return configuracionRepository.findByDireccionId(direccionId)
                .orElseGet(() -> persistirNueva(direccionId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ConfiguracionDireccion guardar(Long direccionId, LocalTime inicioJornada,
            Integer minutosLeccion, String bloquesJornada, List<String> dias) {
        List<BloqueJornada> bloques = Jornada.normalizarBloques(Jornada.parsearBloques(bloquesJornada, true));
        validar(inicioJornada, minutosLeccion, bloques, dias);
        int cantidadLecciones = bloques.stream().mapToInt(BloqueJornada::lecciones).sum();
        validarReduccionContraHorario(direccionId, cantidadLecciones, dias);

        ConfiguracionDireccion config = obtenerOCrear(direccionId);
        config.setInicioJornada(inicioJornada);
        config.setMinutosLeccion(minutosLeccion);
        config.aplicarBloques(bloques);
        config.setDias(dias);
        ConfiguracionDireccion guardada = configuracionRepository.save(config);
        normalizarHoras(direccionId, guardada);
        return guardada;
    }

    private void validar(LocalTime inicioJornada, Integer minutosLeccion,
            List<BloqueJornada> bloques, List<String> dias) {
        if (inicioJornada == null) {
            throw new IllegalArgumentException("Indica la hora de inicio de la jornada");
        }
        if (minutosLeccion == null || minutosLeccion < 20 || minutosLeccion > 90) {
            throw new IllegalArgumentException("La duración de cada lección debe estar entre 20 y 90 minutos");
        }
        if (bloques == null || bloques.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un bloque de lecciones");
        }
        int total = 0;
        for (int i = 0; i < bloques.size(); i++) {
            BloqueJornada bloque = bloques.get(i);
            if (bloque.lecciones() < 1 || bloque.lecciones() > Jornada.MAX_LECCIONES) {
                throw new IllegalArgumentException("Cada bloque debe tener entre 1 y 16 lecciones");
            }
            total += bloque.lecciones();
            boolean ultimo = i == bloques.size() - 1;
            if (ultimo) {
                continue;
            }
            if (bloque.pausa() == TipoPausa.RECESO
                    && (bloque.minutos() < 0 || bloque.minutos() > 60)) {
                throw new IllegalArgumentException("El receso debe estar entre 0 y 60 minutos");
            }
            if (bloque.pausa() == TipoPausa.ALMUERZO
                    && (bloque.minutos() < 0 || bloque.minutos() > 120)) {
                throw new IllegalArgumentException("El almuerzo debe estar entre 0 y 120 minutos");
            }
        }
        if (total < 1 || total > Jornada.MAX_LECCIONES) {
            throw new IllegalArgumentException("La cantidad de lecciones debe estar entre 1 y 16");
        }
        if (dias == null || dias.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un día laboral");
        }
        for (String dia : dias) {
            if (!DiaLaboral.CATALOGO.contains(dia)) {
                throw new IllegalArgumentException("Día laboral inválido");
            }
        }
    }

    private void validarReduccionContraHorario(Long direccionId, Integer cantidadLecciones, List<String> dias) {
        if (horarioRepository.existsByDireccionIdAndNumeroLeccionGreaterThan(direccionId, cantidadLecciones)
                || bloqueoRepository.existsByDireccionIdAndNumeroLeccionGreaterThan(direccionId, cantidadLecciones)) {
            throw new IllegalArgumentException(
                    "Hay lecciones o bloqueos después de la lección " + cantidadLecciones
                            + ". Elimínalos del horario antes de reducir la cantidad.");
        }
        for (String dia : obtener(direccionId).getDias()) {
            if (!dias.contains(dia)
                    && (horarioRepository.existsByDireccionIdAndDia(direccionId, dia)
                            || bloqueoRepository.existsByDireccionIdAndDia(direccionId, dia))) {
                throw new IllegalArgumentException(
                        "Hay asignaciones o bloqueos el "
                                + DiaLaboral.etiqueta(dia)
                                + ". Elimínalos del horario antes de quitar ese día.");
            }
        }
    }

    private void normalizarHoras(Long direccionId, ConfiguracionDireccion config) {
        Jornada jornada = config.jornada();
        List<HorarioLeccion> lecciones = horarioRepository.findByDireccionId(direccionId);
        for (HorarioLeccion leccion : lecciones) {
            leccion.setHoraInicio(jornada.horaInicioLeccion(leccion.getNumeroLeccion()));
            leccion.setHoraFin(jornada.horaFinLeccion(leccion.getNumeroLeccion()));
        }
        horarioRepository.saveAll(lecciones);
    }

    private ConfiguracionDireccion persistirNueva(Long direccionId) {
        try {
            return configuracionRepository.save(
                    ConfiguracionDireccion.predeterminada(direccionDe(direccionId)));
        } catch (DataIntegrityViolationException e) {
            return configuracionRepository.findByDireccionId(direccionId).orElseThrow(() -> e);
        }
    }

    private Direccion direccionDe(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }
}
