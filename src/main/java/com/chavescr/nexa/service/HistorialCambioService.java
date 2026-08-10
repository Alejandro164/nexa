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

    public HistorialCambioService(HistorialCambioRepository repository) {
        this.repository = repository;
    }

    /** Registra un evento de creación/edición/eliminación sobre una definición de Gestión Académica. */
    public void registrar(Long institucionId, Long nivelId, Long materiaId, ModuloAcademico modulo, Long itemId,
            String itemTitulo, AccionHistorial accion, Long usuarioId, String usuarioNombre) {
        registrar(institucionId, nivelId, materiaId, modulo, itemId, itemTitulo, accion, usuarioId, usuarioNombre, null);
    }

    /** Igual que {@link #registrar}, pero con un detalle libre (ej. lista de estudiantes calificados y su nota). */
    public void registrar(Long institucionId, Long nivelId, Long materiaId, ModuloAcademico modulo, Long itemId,
            String itemTitulo, AccionHistorial accion, Long usuarioId, String usuarioNombre, String detalle) {
        HistorialCambio evento = new HistorialCambio();
        evento.setInstitucionId(institucionId);
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
    }

    @Transactional(readOnly = true)
    public List<HistorialCambio> listar(Long institucionId, Long nivelId, Long materiaId) {
        return repository.findByInstitucionIdAndNivelIdAndMateriaIdOrderByFechaDesc(institucionId, nivelId, materiaId);
    }
}
