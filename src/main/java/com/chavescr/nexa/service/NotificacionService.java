package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Notificacion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.NotificacionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void crear(Long usuarioId, String mensaje, String enlace) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        crear(usuario, mensaje, enlace);
    }

    public void crear(Usuario usuario, String mensaje, String enlace) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        notificacionRepository.save(nueva(usuario, mensaje, enlace));
    }

    public void crearTodas(List<Usuario> destinatarios, String mensaje, String enlace) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return;
        }
        List<Notificacion> lote = new ArrayList<>(destinatarios.size());
        for (Usuario usuario : destinatarios) {
            lote.add(nueva(usuario, mensaje, enlace));
        }
        guardarTodas(lote);
    }

    public Notificacion nueva(Usuario usuario, String mensaje, String enlace) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setMensaje(mensaje);
        notificacion.setEnlace(enlace);
        return notificacion;
    }

    public void guardarTodas(List<Notificacion> lote) {
        if (lote == null || lote.isEmpty()) {
            return;
        }
        notificacionRepository.saveAll(lote);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarNoLeidas(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /**
     * Deja una sola notificación sin leer por enlace: la anterior se marca como leída y se crea una
     * nueva con el texto actualizado. Así una ráfaga de mensajes no llena la campanita de avisos.
     */
    public void reemplazarPorEnlace(Usuario usuario, String mensaje, String enlace) {
        notificacionRepository.findFirstByUsuarioIdAndEnlaceAndLeidaFalse(usuario.getId(), enlace)
                .ifPresent(anterior -> {
                    anterior.setLeida(true);
                    notificacionRepository.save(anterior);
                });
        notificacionRepository.save(nueva(usuario, mensaje, enlace));
    }

    public void marcarLeidasPorEnlace(Long usuarioId, String enlace) {
        notificacionRepository.marcarLeidasPorEnlace(usuarioId, enlace);
    }

    public void marcarLeida(Long notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }
}
