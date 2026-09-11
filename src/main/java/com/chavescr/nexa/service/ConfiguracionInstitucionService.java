package com.chavescr.nexa.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.ConfiguracionInstitucion;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.repository.ConfiguracionInstitucionRepository;
import com.chavescr.nexa.repository.DocenteBloqueoLeccionRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.InstitucionRepository;

@Service
@Transactional
public class ConfiguracionInstitucionService {

    private final ConfiguracionInstitucionRepository configuracionRepository;
    private final InstitucionRepository institucionRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final DocenteBloqueoLeccionRepository bloqueoRepository;

    public ConfiguracionInstitucionService(ConfiguracionInstitucionRepository configuracionRepository,
            InstitucionRepository institucionRepository,
            HorarioLeccionRepository horarioRepository,
            DocenteBloqueoLeccionRepository bloqueoRepository) {
        this.configuracionRepository = configuracionRepository;
        this.institucionRepository = institucionRepository;
        this.horarioRepository = horarioRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ConfiguracionInstitucion obtener(Long institucionId) {
        return configuracionRepository.findByInstitucionId(institucionId)
                .orElseGet(() -> ConfiguracionInstitucion.predeterminada(null));
    }

    @Transactional(rollbackFor = Exception.class)
    public ConfiguracionInstitucion obtenerOCrear(Long institucionId) {
        return configuracionRepository.findByInstitucionId(institucionId)
                .orElseGet(() -> persistirNueva(institucionId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ConfiguracionInstitucion guardar(Long institucionId, Integer cantidadLecciones, LocalTime inicioJornada,
            Integer minutosLeccion, Integer minutosRecreo, Integer leccionesPorBloque,
            Integer leccionAlmuerzo, Integer minutosAlmuerzo, List<String> dias) {
        if (minutosAlmuerzo == null) {
            minutosAlmuerzo = ConfiguracionInstitucion.MINUTOS_ALMUERZO_PREDETERMINADOS;
        }
        if (leccionAlmuerzo == null && minutosAlmuerzo > 0) {
            leccionAlmuerzo = ConfiguracionInstitucion.LECCION_ALMUERZO_PREDETERMINADA;
        }
        validar(cantidadLecciones, inicioJornada, minutosLeccion, minutosRecreo, leccionesPorBloque,
                leccionAlmuerzo, minutosAlmuerzo, dias);
        validarReduccionContraHorario(institucionId, cantidadLecciones, dias);

        ConfiguracionInstitucion config = obtenerOCrear(institucionId);
        config.setCantidadLecciones(cantidadLecciones);
        config.setInicioJornada(inicioJornada);
        config.setMinutosLeccion(minutosLeccion);
        config.setMinutosRecreo(minutosRecreo);
        config.setLeccionesPorBloque(leccionesPorBloque);
        config.setLeccionAlmuerzo(leccionAlmuerzo);
        config.setMinutosAlmuerzo(minutosAlmuerzo);
        config.setDias(dias);
        ConfiguracionInstitucion guardada = configuracionRepository.save(config);
        normalizarHoras(institucionId, guardada);
        return guardada;
    }

    private void validar(Integer cantidadLecciones, LocalTime inicioJornada, Integer minutosLeccion,
            Integer minutosRecreo, Integer leccionesPorBloque, Integer leccionAlmuerzo,
            Integer minutosAlmuerzo, List<String> dias) {
        if (cantidadLecciones == null || cantidadLecciones < 1 || cantidadLecciones > 16) {
            throw new IllegalArgumentException("La cantidad de lecciones debe estar entre 1 y 16");
        }
        if (inicioJornada == null) {
            throw new IllegalArgumentException("Indica la hora de inicio de la jornada");
        }
        if (minutosLeccion == null || minutosLeccion < 20 || minutosLeccion > 90) {
            throw new IllegalArgumentException("La duración de cada lección debe estar entre 20 y 90 minutos");
        }
        if (minutosRecreo == null || minutosRecreo < 0 || minutosRecreo > 60) {
            throw new IllegalArgumentException("El recreo debe estar entre 0 y 60 minutos");
        }
        if (leccionesPorBloque == null || leccionesPorBloque < 1 || leccionesPorBloque > cantidadLecciones) {
            throw new IllegalArgumentException("Las lecciones por bloque deben estar entre 1 y la cantidad total");
        }
        if (minutosAlmuerzo == null || minutosAlmuerzo < 0 || minutosAlmuerzo > 120) {
            throw new IllegalArgumentException("El almuerzo debe estar entre 0 y 120 minutos");
        }
        if (minutosAlmuerzo > 0 && (leccionAlmuerzo == null || leccionAlmuerzo < 1
                || leccionAlmuerzo >= cantidadLecciones)) {
            throw new IllegalArgumentException(
                    "El almuerzo debe quedar después de una lección que no sea la última");
        }
        if (dias == null || dias.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un día laboral");
        }
        for (String dia : dias) {
            if (!ConfiguracionInstitucion.DIAS_CATALOGO.contains(dia)) {
                throw new IllegalArgumentException("Día laboral inválido");
            }
        }
    }

    private void validarReduccionContraHorario(Long institucionId, Integer cantidadLecciones, List<String> dias) {
        if (horarioRepository.existsByInstitucionIdAndNumeroLeccionGreaterThan(institucionId, cantidadLecciones)
                || bloqueoRepository.existsByInstitucionIdAndNumeroLeccionGreaterThan(institucionId, cantidadLecciones)) {
            throw new IllegalArgumentException(
                    "Hay lecciones o bloqueos después de la lección " + cantidadLecciones
                            + ". Elimínalos del horario antes de reducir la cantidad.");
        }
        for (String dia : obtener(institucionId).getDias()) {
            if (!dias.contains(dia)
                    && (horarioRepository.existsByInstitucionIdAndDia(institucionId, dia)
                            || bloqueoRepository.existsByInstitucionIdAndDia(institucionId, dia))) {
                throw new IllegalArgumentException(
                        "Hay asignaciones o bloqueos el "
                                + ConfiguracionInstitucion.etiquetaDia(dia)
                                + ". Elimínalos del horario antes de quitar ese día.");
            }
        }
    }

    private void normalizarHoras(Long institucionId, ConfiguracionInstitucion config) {
        List<HorarioLeccion> lecciones = horarioRepository.findByInstitucionId(institucionId);
        lecciones.forEach(config::aplicarHorario);
        horarioRepository.saveAll(lecciones);
    }

    private ConfiguracionInstitucion persistirNueva(Long institucionId) {
        try {
            return configuracionRepository.save(
                    ConfiguracionInstitucion.predeterminada(institucionDe(institucionId)));
        } catch (DataIntegrityViolationException e) {
            return configuracionRepository.findByInstitucionId(institucionId).orElseThrow(() -> e);
        }
    }

    private Institucion institucionDe(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }
}
