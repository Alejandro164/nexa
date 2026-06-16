package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    public List<Visita> obtenerVisitasDelDia(Long institucionId) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return visitaRepository.findByInstitucionIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(
                institucionId, inicio, fin);
    }

    public List<Visita> obtenerTodasPorInstitucion(Long institucionId) {
        return visitaRepository.findByInstitucionIdOrderByFechaRegistroDesc(institucionId);
    }

    public List<Visita> buscarPorFiltro(String filtro, Long institucionId) {
        return visitaRepository.buscarPorFiltro(filtro, institucionId);
    }

    public Visita registrarVisita(Visita visita, Long institucionId) {
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));
        visita.setInstitucion(institucion);
        return visitaRepository.save(visita);
    }

    public Visita autorizarEntrada(Long visitaId) {
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RuntimeException("Visita no encontrada"));
        visita.setEstado(Visita.EstadoVisita.AUTORIZADA);
        visita.setFechaHoraIngreso(LocalDateTime.now());
        return visitaRepository.save(visita);
    }

    public Visita denegarEntrada(Long visitaId, String observaciones) {
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RuntimeException("Visita no encontrada"));
        visita.setEstado(Visita.EstadoVisita.DENEGADA);
        if (observaciones != null && !observaciones.isBlank()) {
            visita.setObservaciones(observaciones);
        }
        return visitaRepository.save(visita);
    }

    public Visita registrarSalida(Long visitaId) {
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new RuntimeException("Visita no encontrada"));
        visita.setEstado(Visita.EstadoVisita.FINALIZADA);
        visita.setFechaHoraSalida(LocalDateTime.now());
        return visitaRepository.save(visita);
    }
}
