package com.chavescr.nexa.service;

import java.text.Normalizer;
import java.util.Locale;

/** Formatos de texto que comparten la mensajería y Comunicados. */
final class FormatoVista {

    private static final Locale ES = Locale.forLanguageTag("es-CR");

    private FormatoVista() {
    }

    /** "186 KB", "1,4 MB"; vacío si no se conoce el tamaño. */
    static String tamano(Long bytes) {
        if (bytes == null) {
            return "";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return String.format(ES, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** Minúsculas y sin tildes, para buscar sin importar acentos ni mayúsculas. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(ES).strip();
    }

    static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }
}
