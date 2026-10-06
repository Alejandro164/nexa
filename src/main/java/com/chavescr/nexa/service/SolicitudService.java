package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.entity.Solicitud;
import com.chavescr.nexa.repository.SolicitudRepository;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    public List<Solicitud> listarPorDireccion(Long direccionId) {
        return solicitudRepository.findByDireccionIdOrderByFechaSolicitudDesc(direccionId);
    }

    public Solicitud marcarEnProceso(Long id) {
        Solicitud solicitud = obtenerPorId(id);
        solicitud.setEstado(Solicitud.EstadoSolicitud.EN_PROCESO);
        return solicitudRepository.save(solicitud);
    }

    public Solicitud resolver(Long id, String respuesta) {
        Solicitud solicitud = obtenerPorId(id);
        solicitud.setEstado(Solicitud.EstadoSolicitud.RESUELTA);
        solicitud.setRespuesta(respuesta);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    public Solicitud rechazar(Long id, String respuesta) {
        Solicitud solicitud = obtenerPorId(id);
        solicitud.setEstado(Solicitud.EstadoSolicitud.RECHAZADA);
        solicitud.setRespuesta(respuesta);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    private Solicitud obtenerPorId(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
    }
}
