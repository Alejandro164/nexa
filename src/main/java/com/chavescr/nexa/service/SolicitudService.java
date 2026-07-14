package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Solicitud;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.SolicitudRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    public List<Solicitud> listarPorPadre(Long padreId) {
        return solicitudRepository.findByPadreIdOrderByFechaSolicitudDesc(padreId);
    }

    public List<Solicitud> listarPorInstitucion(Long institucionId) {
        return solicitudRepository.findByInstitucionIdOrderByFechaSolicitudDesc(institucionId);
    }

    public Solicitud crearSolicitud(Long padreId, Solicitud.TipoSolicitud tipo, Long estudianteId,
            Long docenteId, String detalle, Long institucionId) {
        Usuario padre = usuarioRepository.findById(padreId)
                .orElseThrow(() -> new IllegalArgumentException("Padre no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        boolean requiereEstudiante = tipo == Solicitud.TipoSolicitud.CONSTANCIA_MATRICULA
                || tipo == Solicitud.TipoSolicitud.CITA_DOCENTE;
        if (requiereEstudiante && estudianteId == null) {
            throw new IllegalArgumentException("Debe seleccionar un hijo/a para este tipo de solicitud");
        }

        Solicitud solicitud = new Solicitud();
        solicitud.setPadre(padre);
        solicitud.setTipo(tipo);
        solicitud.setDetalle(detalle);
        solicitud.setInstitucion(institucion);

        if (estudianteId != null) {
            boolean esHijo = usuarioRepository.findEstudiantesByPadreId(padreId).stream()
                    .anyMatch(e -> e.getId().equals(estudianteId));
            if (!esHijo) {
                throw new AccessDeniedException("Solo puede solicitar trámites para sus propios hijos");
            }
            solicitud.setEstudiante(usuarioRepository.findById(estudianteId).orElseThrow(
                    () -> new IllegalArgumentException("Estudiante no encontrado")));
        }

        if (tipo == Solicitud.TipoSolicitud.CITA_DOCENTE) {
            if (docenteId == null) {
                throw new IllegalArgumentException("Debe seleccionar un docente para solicitar una cita");
            }
            solicitud.setDocente(usuarioRepository.findById(docenteId).orElseThrow(
                    () -> new IllegalArgumentException("Docente no encontrado")));
        }

        return solicitudRepository.save(solicitud);
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
