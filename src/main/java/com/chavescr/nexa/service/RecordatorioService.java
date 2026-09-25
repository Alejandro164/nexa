package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Recordatorio;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.RecordatorioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);

    private final RecordatorioRepository recordatorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;

    public RecordatorioService(RecordatorioRepository recordatorioRepository,
            UsuarioRepository usuarioRepository,
            DireccionRepository direccionRepository) {
        this.recordatorioRepository = recordatorioRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true)
    public List<Recordatorio> listarRecordatorios(Long direccionId, Long usuarioId) {
        return recordatorioRepository.findByUsuarioIdAndDireccionIdOrderByFechaLimiteAsc(usuarioId, direccionId);
    }

    @Transactional(readOnly = true)
    public Recordatorio obtenerRecordatorio(Long direccionId, Long usuarioId, Long id) {
        return recordatorioRepository.findByIdAndUsuarioIdAndDireccionId(id, usuarioId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Recordatorio no encontrado"));
    }

    public Recordatorio guardarRecordatorio(Long direccionId, Long usuarioId, Long id,
            String titulo, String descripcion, LocalDateTime fechaLimite,
            Recordatorio.EstadoRecordatorio estado) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (fechaLimite == null) {
            throw new IllegalArgumentException("La fecha límite es obligatoria");
        }
        Recordatorio recordatorio = id == null
                ? new Recordatorio()
                : obtenerRecordatorio(direccionId, usuarioId, id);
        if (recordatorio.getId() == null) {
            recordatorio.setUsuario(obtenerUsuario(usuarioId));
            recordatorio.setDireccion(obtenerDireccion(direccionId));
        }
        recordatorio.setTitulo(titulo.trim());
        recordatorio.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
        recordatorio.setFechaLimite(fechaLimite);
        recordatorio.setEstado(estado != null ? estado : Recordatorio.EstadoRecordatorio.PENDIENTE);
        Recordatorio guardado = recordatorioRepository.save(recordatorio);
        log.info("Recordatorio guardado: id={}, titulo={}", guardado.getId(), guardado.getTitulo());
        return guardado;
    }

    public void eliminarRecordatorio(Long direccionId, Long usuarioId, Long id) {
        Recordatorio recordatorio = obtenerRecordatorio(direccionId, usuarioId, id);
        recordatorioRepository.delete(recordatorio);
        log.info("Recordatorio eliminado: id={}", id);
    }

    public void cambiarEstadoRecordatorio(Long direccionId, Long usuarioId, Long id,
            Recordatorio.EstadoRecordatorio nuevoEstado) {
        Recordatorio recordatorio = obtenerRecordatorio(direccionId, usuarioId, id);
        recordatorio.setEstado(nuevoEstado);
        recordatorioRepository.save(recordatorio);
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Direccion obtenerDireccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }
}
