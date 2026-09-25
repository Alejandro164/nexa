package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.HistorialCambio;
import com.chavescr.nexa.entity.ModuloAcademico;
import com.chavescr.nexa.repository.HistorialCambioRepository;

@Service
@Transactional
public class HistorialCambioService {

    private final HistorialCambioRepository repository;
    private final BitacoraService bitacoraService;

    public HistorialCambioService(HistorialCambioRepository repository, BitacoraService bitacoraService) {
        this.repository = repository;
        this.bitacoraService = bitacoraService;
    }

    /** Registra un evento de creación/edición/eliminación sobre una definición de Gestión Académica. */
    public void registrar(Long direccionId, Long nivelId, Long materiaId, ModuloAcademico modulo, Long itemId,
            String itemTitulo, AccionHistorial accion, Long usuarioId, String usuarioNombre) {
        registrar(direccionId, nivelId, materiaId, modulo, itemId, itemTitulo, accion, usuarioId, usuarioNombre, null);
    }

    /** Igual que {@link #registrar}, pero con un detalle libre (ej. lista de estudiantes calificados y su nota). */
    public void registrar(Long direccionId, Long nivelId, Long materiaId, ModuloAcademico modulo, Long itemId,
            String itemTitulo, AccionHistorial accion, Long usuarioId, String usuarioNombre, String detalle) {
        HistorialCambio evento = new HistorialCambio();
        evento.setDireccionId(direccionId);
        evento.setNivelId(nivelId);
        evento.setMateriaId(materiaId);
        evento.setModulo(modulo);
        evento.setItemId(itemId);
        evento.setItemTitulo(itemTitulo);
        evento.setAccion(accion);
        evento.setUsuarioId(usuarioId);
        evento.setUsuarioNombre(usuarioNombre != null ? usuarioNombre : "Sistema");
        evento.setDetalle(detalle);
        evento.setFecha(LocalDateTime.now());
        repository.save(evento);
        bitacoraService.registrarDesdeHistorialAcademico(direccionId, modulo, itemId, itemTitulo, accion,
                usuarioId, usuarioNombre, detalle);
    }

    @Transactional(readOnly = true)
    public List<HistorialCambio> listar(Long direccionId, Long nivelId, Long materiaId) {
        return repository.findByDireccionIdAndNivelIdAndMateriaIdOrderByFechaDesc(direccionId, nivelId, materiaId);
    }

    /** Últimos cambios académicos (tareas, exámenes, notas, etc.) de la dirección, para el dashboard. */
    @Transactional(readOnly = true)
    public List<HistorialCambio> listarRecientes(Long direccionId) {
        return repository.findTop8ByDireccionIdOrderByFechaDesc(direccionId);
    }
}
