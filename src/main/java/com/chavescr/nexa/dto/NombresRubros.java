package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.ClaveComponente;

/**
 * Nombres y emojis con los que un componente de evaluación muestra sus rubros. Cada componente puede
 * renombrarlos (por ejemplo "Pruebas" → "Exámenes") y elegirles un emoji; los que no se renombran usan el nombre
 * predeterminado, y los que no tienen emoji propio usan el de la institución (ver {@link #emojiDe}).
 */
public class NombresRubros {

    public static final String COTIDIANO = "Cotidiano";
    public static final String TAREAS = "Tareas";
    public static final String PROYECTO = "Proyecto";
    public static final String PRUEBAS = "Pruebas";
    public static final String ASISTENCIA = "Asistencia";

    /** Emojis predeterminados: los mismos que TipoComponenteService le da a cada dirección. */
    public static final String EMOJI_COTIDIANO = "📝";
    public static final String EMOJI_TAREAS = "📚";
    public static final String EMOJI_PROYECTO = "🎯";
    public static final String EMOJI_PRUEBAS = "📋";
    public static final String EMOJI_ASISTENCIA = "📅";

    public static final NombresRubros PREDETERMINADOS =
            new NombresRubros(COTIDIANO, TAREAS, PROYECTO, PRUEBAS, ASISTENCIA);

    private final String cotidiano;
    private final String tareas;
    private final String proyecto;
    private final String pruebas;
    private final String asistencia;
    private final String emojiCotidiano;
    private final String emojiTareas;
    private final String emojiProyecto;
    private final String emojiPruebas;
    private final String emojiAsistencia;

    /** Un nombre null o en blanco toma el predeterminado de ese rubro. Sin emojis propios. */
    public NombresRubros(String cotidiano, String tareas, String proyecto, String pruebas, String asistencia) {
        this(cotidiano, tareas, proyecto, pruebas, asistencia, null, null, null, null, null);
    }

    /** Un emoji null o en blanco significa que el rubro no tiene emoji propio en el componente. */
    public NombresRubros(String cotidiano, String tareas, String proyecto, String pruebas, String asistencia,
            String emojiCotidiano, String emojiTareas, String emojiProyecto, String emojiPruebas,
            String emojiAsistencia) {
        this.cotidiano = o(cotidiano, COTIDIANO);
        this.tareas = o(tareas, TAREAS);
        this.proyecto = o(proyecto, PROYECTO);
        this.pruebas = o(pruebas, PRUEBAS);
        this.asistencia = o(asistencia, ASISTENCIA);
        this.emojiCotidiano = vacioANull(emojiCotidiano);
        this.emojiTareas = vacioANull(emojiTareas);
        this.emojiProyecto = vacioANull(emojiProyecto);
        this.emojiPruebas = vacioANull(emojiPruebas);
        this.emojiAsistencia = vacioANull(emojiAsistencia);
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }

    private static String o(String nombre, String predeterminado) {
        return nombre == null || nombre.isBlank() ? predeterminado : nombre;
    }

    public static String predeterminado(ClaveComponente clave) {
        return PREDETERMINADOS.de(clave);
    }

    public static String emojiPredeterminado(ClaveComponente clave) {
        if (clave == null) {
            return null;
        }
        return switch (clave) {
            case COTIDIANO -> EMOJI_COTIDIANO;
            case TAREA -> EMOJI_TAREAS;
            case PROYECTO -> EMOJI_PROYECTO;
            case EXAMEN -> EMOJI_PRUEBAS;
        };
    }

    /** Emoji propio del rubro en el componente; null si no eligió uno (se usa el de la institución). */
    public String emojiDe(ClaveComponente clave) {
        if (clave == null) {
            return null;
        }
        return switch (clave) {
            case COTIDIANO -> emojiCotidiano;
            case TAREA -> emojiTareas;
            case PROYECTO -> emojiProyecto;
            case EXAMEN -> emojiPruebas;
        };
    }

    /** Emoji y nombre del rubro; sin emoji propio usa {@code emojiInstitucion} (puede ser null). */
    public String etiqueta(ClaveComponente clave, String emojiInstitucion) {
        String emoji = emojiDe(clave);
        if (emoji == null) {
            emoji = emojiInstitucion;
        }
        String nombre = de(clave);
        return emoji == null || emoji.isBlank() ? nombre : emoji + " " + nombre;
    }

    public String getCotidiano() {
        return cotidiano;
    }

    public String getTareas() {
        return tareas;
    }

    public String getProyecto() {
        return proyecto;
    }

    public String getPruebas() {
        return pruebas;
    }

    public String getAsistencia() {
        return asistencia;
    }

    public String de(ClaveComponente clave) {
        if (clave == null) {
            return "Componente";
        }
        return switch (clave) {
            case COTIDIANO -> cotidiano;
            case TAREA -> tareas;
            case PROYECTO -> proyecto;
            case EXAMEN -> pruebas;
        };
    }

    /** Para las plantillas: acepta la clave como texto, incluida "ASISTENCIA". */
    public String de(String clave) {
        if ("ASISTENCIA".equals(clave)) {
            return asistencia;
        }
        try {
            return de(ClaveComponente.valueOf(clave));
        } catch (IllegalArgumentException | NullPointerException e) {
            return "Componente";
        }
    }

    public String getEmojiCotidiano() {
        return emojiCotidiano;
    }

    public String getEmojiTareas() {
        return emojiTareas;
    }

    public String getEmojiProyecto() {
        return emojiProyecto;
    }

    public String getEmojiPruebas() {
        return emojiPruebas;
    }

    public String getEmojiAsistencia() {
        return emojiAsistencia;
    }

    // Etiquetas para encabezados: emoji propio del componente (o el predeterminado) y nombre del rubro.

    public String getEtiquetaCotidiano() {
        return unir(emojiCotidiano, EMOJI_COTIDIANO, cotidiano);
    }

    public String getEtiquetaTareas() {
        return unir(emojiTareas, EMOJI_TAREAS, tareas);
    }

    public String getEtiquetaProyecto() {
        return unir(emojiProyecto, EMOJI_PROYECTO, proyecto);
    }

    public String getEtiquetaPruebas() {
        return unir(emojiPruebas, EMOJI_PRUEBAS, pruebas);
    }

    public String getEtiquetaAsistencia() {
        return unir(emojiAsistencia, EMOJI_ASISTENCIA, asistencia);
    }

    private static String unir(String emoji, String emojiPredeterminado, String nombre) {
        return (emoji != null ? emoji : emojiPredeterminado) + " " + nombre;
    }
}
