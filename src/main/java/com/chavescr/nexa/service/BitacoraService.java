package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaBitacora;
import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.BitacoraEvento;
import com.chavescr.nexa.entity.HistorialCambio;
import com.chavescr.nexa.entity.ModuloAcademico;
import com.chavescr.nexa.entity.ModuloSistema;
import com.chavescr.nexa.repository.BitacoraEventoRepository;
import com.chavescr.nexa.repository.HistorialCambioRepository;

@Service
@Transactional
public class BitacoraService {

    private static final int LIMITE_PANEL = 80;

    private final BitacoraEventoRepository repository;
    private final HistorialCambioRepository historialCambioRepository;

    public BitacoraService(BitacoraEventoRepository repository, HistorialCambioRepository historialCambioRepository) {
        this.repository = repository;
        this.historialCambioRepository = historialCambioRepository;
    }

    public void registrar(Long institucionId, ModuloSistema modulo, String area, AccionHistorial accion, Long itemId,
            String itemTitulo, Long usuarioId, String usuarioNombre) {
        registrar(institucionId, modulo, area, accion, itemId, itemTitulo, usuarioId, usuarioNombre, null);
    }

    public void registrar(Long institucionId, ModuloSistema modulo, String area, AccionHistorial accion, Long itemId,
            String itemTitulo, Long usuarioId, String usuarioNombre, String detalle) {
        if (institucionId == null || modulo == null || accion == null) {
            return;
        }
        BitacoraEvento evento = new BitacoraEvento();
        evento.setInstitucionId(institucionId);
        evento.setModulo(modulo);
        evento.setArea(area);
        evento.setAccion(accion);
        evento.setItemId(itemId);
        evento.setItemTitulo(itemTitulo != null && !itemTitulo.isBlank() ? itemTitulo : "Registro");
        evento.setUsuarioId(usuarioId);
        evento.setUsuarioNombre(usuarioNombre != null ? usuarioNombre : "Sistema");
        evento.setDetalle(detalle);
        evento.setFecha(LocalDateTime.now());
        repository.save(evento);
    }

    /** Copia un evento académico ya persistido para que también aparezca en la bitácora del módulo. */
    public void registrarDesdeHistorialAcademico(Long institucionId, ModuloAcademico academico, Long itemId,
            String itemTitulo, AccionHistorial accion, Long usuarioId, String usuarioNombre, String detalle) {
        registrar(institucionId, ModuloSistema.GESTION_ACADEMICA, ModuloSistema.areaDe(academico), accion, itemId,
                itemTitulo, usuarioId, usuarioNombre, detalle);
    }

    @Transactional(readOnly = true)
    public List<FilaBitacora> listar(Long institucionId, ModuloSistema modulo, AccionHistorial accion) {
        PageRequest limite = PageRequest.of(0, LIMITE_PANEL);
        List<BitacoraEvento> eventos = modulo == null
                ? (accion == null
                        ? repository.findByInstitucionIdOrderByFechaDesc(institucionId, limite)
                        : repository.findByInstitucionIdAndAccionOrderByFechaDesc(institucionId, accion, limite))
                : (accion == null
                        ? repository.findByInstitucionIdAndModuloOrderByFechaDesc(institucionId, modulo, limite)
                        : repository.findByInstitucionIdAndModuloAndAccionOrderByFechaDesc(institucionId, modulo,
                                accion, limite));

        List<FilaBitacora> filas = new ArrayList<>(eventos.stream().map(FilaBitacora::desde).toList());

        if (modulo == null || modulo == ModuloSistema.GESTION_ACADEMICA) {
            incorporarHistorialAcademicoPrevio(institucionId, accion, filas);
        }

        filas.sort(Comparator.comparing(FilaBitacora::getFecha).reversed());
        if (filas.size() > LIMITE_PANEL) {
            return filas.subList(0, LIMITE_PANEL);
        }
        return filas;
    }

    /**
     * El historial académico existía antes de la bitácora global. Se mezcla aquí para no perder
     * creaciones/ediciones ya guardadas, evitando duplicar las que también se escribieron en bitácora.
     */
    private void incorporarHistorialAcademicoPrevio(Long institucionId, AccionHistorial accion, List<FilaBitacora> filas) {
        List<HistorialCambio> historial = historialCambioRepository.findByInstitucionIdOrderByFechaDesc(institucionId);
        for (HistorialCambio evento : historial) {
            if (accion != null && evento.getAccion() != accion) {
                continue;
            }
            FilaBitacora fila = FilaBitacora.desde(evento);
            if (filas.stream().noneMatch(existente -> esMismoEvento(existente, fila))) {
                filas.add(fila);
            }
        }
    }

    private boolean esMismoEvento(FilaBitacora a, FilaBitacora b) {
        return a.getModulo() == b.getModulo()
                && a.getAccion() == b.getAccion()
                && iguales(a.getItemTitulo(), b.getItemTitulo())
                && iguales(a.getUsuarioNombre(), b.getUsuarioNombre())
                && a.getFecha() != null && b.getFecha() != null
                && a.getFecha().withNano(0).equals(b.getFecha().withNano(0));
    }

    private boolean iguales(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equals(b);
    }
}
