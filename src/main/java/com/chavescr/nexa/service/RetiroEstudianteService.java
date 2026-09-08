package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.RetiroEstudiante;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.RetiroEstudianteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
public class RetiroEstudianteService {

    @Autowired
    private RetiroEstudianteRepository retiroEstudianteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private NotificacionService notificacionService;

    public List<RetiroEstudiante> obtenerRetirosDelDia(Long institucionId) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return retiroEstudianteRepository.findByInstitucionIdAndFechaHoraSolicitudBetweenOrderByFechaHoraSolicitudDesc(
                institucionId, inicio, fin);
    }

    public List<RetiroEstudiante> obtenerRetirosDelPadre(Long padreId) {
        return retiroEstudianteRepository.findByPadreIdOrderByFechaHoraSolicitudDesc(padreId);
    }

    /**
     * Un padre solo puede solicitar el retiro de un estudiante que
     * realmente tenga asignado (relación real padres_estudiantes).
     */
    public RetiroEstudiante solicitarRetiro(Long padreId, Long estudianteId, String motivo, Long institucionId) {
        Usuario padre = usuarioRepository.findById(padreId)
                .orElseThrow(() -> new IllegalArgumentException("Padre no encontrado"));
        boolean esHijo = usuarioRepository.findEstudiantesByPadreId(padreId).stream()
                .anyMatch(e -> e.getId().equals(estudianteId));
        if (!esHijo) {
            throw new AccessDeniedException("Solo puede solicitar el retiro de sus propios hijos");
        }
        Usuario estudiante = usuarioRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        RetiroEstudiante retiro = new RetiroEstudiante();
        retiro.setPadre(padre);
        retiro.setEstudiante(estudiante);
        retiro.setMotivo(motivo);
        retiro.setInstitucion(institucion);
        return retiroEstudianteRepository.save(retiro);
    }

    public RetiroEstudiante autorizar(Long retiroId) {
        RetiroEstudiante retiro = obtenerPorId(retiroId);
        exigirEstado(retiro, RetiroEstudiante.EstadoRetiro.PENDIENTE, "autorizar");
        retiro.setEstado(RetiroEstudiante.EstadoRetiro.AUTORIZADO);
        RetiroEstudiante guardado = retiroEstudianteRepository.save(retiro);
        notificacionService.crear(guardado.getPadre().getId(),
                "Tu solicitud de retiro de " + guardado.getEstudiante().getNombre() + " fue autorizada.",
                "/portal-padres");
        return guardado;
    }

    public RetiroEstudiante denegar(Long retiroId, String observaciones) {
        RetiroEstudiante retiro = obtenerPorId(retiroId);
        exigirEstado(retiro, RetiroEstudiante.EstadoRetiro.PENDIENTE, "denegar");
        retiro.setEstado(RetiroEstudiante.EstadoRetiro.DENEGADO);
        if (observaciones != null && !observaciones.isBlank()) {
            retiro.setObservaciones(observaciones);
        }
        RetiroEstudiante guardado = retiroEstudianteRepository.save(retiro);
        notificacionService.crear(guardado.getPadre().getId(),
                "Tu solicitud de retiro de " + guardado.getEstudiante().getNombre() + " fue denegada.",
                "/portal-padres");
        return guardado;
    }

    public RetiroEstudiante registrarSalida(Long retiroId, String retiradoPorNombre, String retiradoPorIdentificacion) {
        RetiroEstudiante retiro = obtenerPorId(retiroId);
        exigirEstado(retiro, RetiroEstudiante.EstadoRetiro.AUTORIZADO, "registrar la salida de");
        retiro.setEstado(RetiroEstudiante.EstadoRetiro.FINALIZADO);
        retiro.setFechaHoraSalida(LocalDateTime.now());
        retiro.setRetiradoPorNombre(
                retiradoPorNombre != null && !retiradoPorNombre.isBlank() ? retiradoPorNombre
                        : retiro.getPadre().getNombre());
        retiro.setRetiradoPorIdentificacion(retiradoPorIdentificacion);
        return retiroEstudianteRepository.save(retiro);
    }

    private RetiroEstudiante obtenerPorId(Long id) {
        return retiroEstudianteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de retiro no encontrada"));
    }

    private void exigirEstado(RetiroEstudiante retiro, RetiroEstudiante.EstadoRetiro esperado, String accion) {
        if (retiro.getEstado() != esperado) {
            throw new IllegalStateException("No se puede " + accion + " este retiro: ya está en estado "
                    + retiro.getEstado() + ".");
        }
    }
}
