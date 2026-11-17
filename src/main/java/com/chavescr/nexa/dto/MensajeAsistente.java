package com.chavescr.nexa.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Un mensaje de la conversación con el asistente. Vive solo en la sesión (en prod la sesión está en Redis,
 * por eso es Serializable). Los mensajes de error se muestran pero no se le reenvían al modelo.
 */
public record MensajeAsistente(Rol rol, String texto, LocalDateTime fecha, boolean error) implements Serializable {

    public enum Rol { USUARIO, ASISTENTE }

    public static MensajeAsistente delUsuario(String texto) {
        return new MensajeAsistente(Rol.USUARIO, texto, LocalDateTime.now(), false);
    }

    public static MensajeAsistente delAsistente(String texto) {
        return new MensajeAsistente(Rol.ASISTENTE, texto, LocalDateTime.now(), false);
    }

    public static MensajeAsistente deError(String texto) {
        return new MensajeAsistente(Rol.ASISTENTE, texto, LocalDateTime.now(), true);
    }

    public boolean isDelUsuario() {
        return rol == Rol.USUARIO;
    }
}
