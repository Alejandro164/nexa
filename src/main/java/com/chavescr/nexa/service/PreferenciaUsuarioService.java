package com.chavescr.nexa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.PreferenciaUsuario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.PreferenciaUsuarioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class PreferenciaUsuarioService {

    private final PreferenciaUsuarioRepository preferenciaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PreferenciaUsuarioService(PreferenciaUsuarioRepository preferenciaUsuarioRepository,
            UsuarioRepository usuarioRepository) {
        this.preferenciaUsuarioRepository = preferenciaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Las preferencias del usuario, o los valores por defecto si nunca las ha guardado. */
    @Transactional(readOnly = true)
    public PreferenciaUsuario obtenerPorUsuario(Long usuarioId) {
        return preferenciaUsuarioRepository.findByUsuarioId(usuarioId)
                .orElseGet(PreferenciaUsuario::new);
    }

    public PreferenciaUsuario guardar(Long usuarioId, boolean notifEmail, boolean notifPush,
            boolean notifRecordatorios, boolean notifResumenSemanal, String idioma, String zonaHoraria,
            String tema) {
        PreferenciaUsuario preferencia = preferenciaUsuarioRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    PreferenciaUsuario nueva = new PreferenciaUsuario();
                    Usuario usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                    nueva.setUsuario(usuario);
                    return nueva;
                });
        preferencia.setNotifEmail(notifEmail);
        preferencia.setNotifPush(notifPush);
        preferencia.setNotifRecordatorios(notifRecordatorios);
        preferencia.setNotifResumenSemanal(notifResumenSemanal);
        preferencia.setIdioma(idioma);
        preferencia.setZonaHoraria(zonaHoraria);
        preferencia.setTema(tema);
        return preferenciaUsuarioRepository.save(preferencia);
    }
}
