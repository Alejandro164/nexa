package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.entity.RetiroEstudiante;
import com.chavescr.nexa.repository.RetiroEstudianteRepository;

@Service
public class RetiroEstudianteService {

    @Autowired
    private RetiroEstudianteRepository retiroEstudianteRepository;

    @Autowired
    private NotificacionService notificacionService;

    public List<RetiroEstudiante> obtenerRetirosDelDia(Long direccionId) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return retiroEstudianteRepository.findByDireccionIdAndFechaHoraSolicitudBetweenOrderByFechaHoraSolicitudDesc(
                direccionId, inicio, fin);
    }

    public RetiroEstudiante autorizar(Long retiroId) {
        RetiroEstudiante retiro = obtenerPorId(retiroId);
        exigirEstado(retiro, RetiroEstudiante.EstadoRetiro.PENDIENTE, "autorizar");
        retiro.setEstado(RetiroEstudiante.EstadoRetiro.AUTORIZADO);
        RetiroEstudiante guardado = retiroEstudianteRepository.save(retiro);
        notificacionService.crear(guardado.getPadre().getId(),
                "Tu solicitud de retiro de " + guardado.getEstudiante().getNombre() + " fue autorizada.",
                "/inicio");
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
                "/inicio");
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
