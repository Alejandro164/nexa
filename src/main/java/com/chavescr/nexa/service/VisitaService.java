package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Visita;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.VisitaRepository;

@Service
public class VisitaService {

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private NotificacionService notificacionService;

    public List<Visita> obtenerVisitasDelDia(Long institucionId) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return visitaRepository.findByInstitucionIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(
                institucionId, inicio, fin);
    }

    public List<Visita> obtenerVisitasPorRango(Long institucionId, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        return visitaRepository.findByInstitucionIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(
                institucionId, inicio, fin);
    }

    public List<Visita> buscarPorFiltro(String filtro, Long institucionId) {
        return visitaRepository.buscarPorFiltro(filtro, institucionId);
    }

    /** Visita más reciente de un visitante ya registrado antes (no necesariamente padre), para autocompletar. */
    public Optional<Visita> buscarVisitanteRecurrente(String identificacion, Long institucionId) {
        return visitaRepository.findByIdentificacionAndInstitucionIdOrderByFechaRegistroDesc(identificacion, institucionId)
                .stream().findFirst();
    }

    public Visita registrarVisita(Visita visita, Long institucionId) {
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));
        visita.setInstitucion(institucion);
        return visitaRepository.save(visita);
    }

    public Visita autorizarEntrada(Long visitaId) {
        Visita visita = obtenerPorId(visitaId);
        exigirEstado(visita, Visita.EstadoVisita.PENDIENTE, "autorizar");
        visita.setEstado(Visita.EstadoVisita.AUTORIZADA);
        visita.setFechaHoraIngreso(LocalDateTime.now());
        Visita guardada = visitaRepository.save(visita);
        if (guardada.getPersonaVisitada() != null) {
            notificacionService.crear(guardada.getPersonaVisitada().getId(),
                    guardada.getNombreVisitante() + " llegó a visitarte y fue autorizado a ingresar.",
                    "/control-de-acceso");
        }
        return guardada;
    }

    public Visita denegarEntrada(Long visitaId, String observaciones) {
        Visita visita = obtenerPorId(visitaId);
        exigirEstado(visita, Visita.EstadoVisita.PENDIENTE, "denegar");
        visita.setEstado(Visita.EstadoVisita.DENEGADA);
        if (observaciones != null && !observaciones.isBlank()) {
            visita.setObservaciones(observaciones);
        }
        return visitaRepository.save(visita);
    }

    public Visita registrarSalida(Long visitaId) {
        Visita visita = obtenerPorId(visitaId);
        exigirEstado(visita, Visita.EstadoVisita.AUTORIZADA, "registrar la salida de");
        visita.setEstado(Visita.EstadoVisita.FINALIZADA);
        visita.setFechaHoraSalida(LocalDateTime.now());
        return visitaRepository.save(visita);
    }

    private Visita obtenerPorId(Long visitaId) {
        return visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RuntimeException("Visita no encontrada"));
    }

    private void exigirEstado(Visita visita, Visita.EstadoVisita esperado, String accion) {
        if (visita.getEstado() != esperado) {
            throw new IllegalStateException("No se puede " + accion + " esta visita: ya está en estado "
                    + visita.getEstado() + ".");
        }
    }
}
