package com.chavescr.nexa.service;

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
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setMensaje(mensaje);
        notificacion.setEnlace(enlace);
        notificacionRepository.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarNoLeidas(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    public void marcarLeida(Long notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }
}
