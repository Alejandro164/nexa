package com.chavescr.nexa.dto;

import java.nio.file.Path;
import java.util.List;

/**
 * Datos ya preparados para pintar la mensajería. Los registros se leen en las plantillas con la
 * sintaxis de método ({@code m.texto()}).
 */
public final class MensajeriaVista {

    private MensajeriaVista() {
    }

    /**
     * Fila de la lista de conversaciones. {@code direccion} solo viene cuando la conversación es de una
     * dirección distinta de la activa.
     */
    public record Resumen(Long id, String nombre, String iniciales, String rol, String claseRol, String preview,
            String hora, long noLeidos, String direccion) {

        public boolean nueva() {
            return noLeidos > 0;
        }
    }

    public record Cita(String autor, String texto) {
    }

    public record Archivo(Long mensajeId, String nombre, String tamano, boolean imagen) {
    }

    public record Reaccion(String emoji, long cantidad, boolean yo, String titulo) {
    }

    public record Mensaje(Long id, boolean mio, String autor, String iniciales, String claseRol, String dia,
            String hora, String texto, Cita cita, Archivo archivo, List<Reaccion> reacciones, boolean leido) {

        /** Lo que se muestra al responder: el texto o, si solo hay archivo, su nombre. */
        public String extracto() {
            if (texto != null && !texto.isBlank()) {
                return texto;
            }
            return archivo != null ? archivo.nombre() : "";
        }
    }

    /** Conversación abierta: cabecera, mensajes y la "firma" que usa el sondeo para saber si hay cambios. */
    public record Hilo(Long id, String nombre, String iniciales, String rol, String claseRol, String direccion,
            List<Mensaje> mensajes, String firma) {
    }

    /** Persona a la que se puede escribir (selector de "Nuevo"). */
    public record Contacto(Long id, String nombre, String iniciales, String rol, String claseRol) {
    }

    /** Archivo adjunto listo para servirse. */
    public record ArchivoDescarga(Path ruta, String nombre, String tipoContenido, boolean imagen) {
    }
}
