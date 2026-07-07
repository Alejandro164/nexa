package com.chavescr.nexa.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Representa un evento unificado para el calendario de la Agenda,
 * proveniente de Tareas, Recordatorios o Actividades Institucionales (MEP).
 */
public class EventoCalendarioDTO {

    private static final Locale ES = new Locale("es");

    public enum Tipo { TAREA, RECORDATORIO, INSTITUCIONAL }

    private final Tipo tipo;
    private final String titulo;
    private final String descripcion;
    private final LocalDate fecha;
    private final LocalTime hora;
    private final String enlace;
    private final boolean multiDia;

    private EventoCalendarioDTO(Tipo tipo, String titulo, String descripcion,
            LocalDate fecha, LocalTime hora, String enlace, boolean multiDia) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.hora = hora;
        this.enlace = enlace;
        this.multiDia = multiDia;
    }

    public static EventoCalendarioDTO deTarea(String titulo, String descripcion, LocalDate fecha) {
        return new EventoCalendarioDTO(Tipo.TAREA, titulo, descripcion, fecha, null, null, false);
    }

    public static EventoCalendarioDTO deRecordatorio(String titulo, String descripcion, LocalDate fecha, LocalTime hora) {
        return new EventoCalendarioDTO(Tipo.RECORDATORIO, titulo, descripcion, fecha, hora, null, false);
    }

    public static EventoCalendarioDTO deInstitucional(String titulo, String descripcion, LocalDate fecha,
            String enlace, boolean multiDia) {
        return new EventoCalendarioDTO(Tipo.INSTITUCIONAL, titulo, descripcion, fecha, null, enlace, multiDia);
    }

    public Tipo getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public String getEnlace() { return enlace; }

    /** true si proviene de un evento institucional de varios días (se muestra como banda, no como chip en la grilla). */
    public boolean isMultiDia() { return multiDia; }

    public String getCssClass() {
        return switch (tipo) {
            case TAREA -> "event-tarea";
            case RECORDATORIO -> "event-recordatorio";
            case INSTITUCIONAL -> "event-general";
        };
    }

    public String getIcono() {
        return switch (tipo) {
            case TAREA -> "✅";
            case RECORDATORIO -> "⏰";
            case INSTITUCIONAL -> "📅";
        };
    }

    public boolean isTieneHora() {
        return hora != null;
    }

    public boolean isTieneEnlace() {
        return enlace != null && !enlace.isBlank();
    }

    public String getTipoLabel() {
        return switch (tipo) {
            case TAREA -> "Tarea";
            case RECORDATORIO -> "Recordatorio";
            case INSTITUCIONAL -> "Evento Institucional";
        };
    }

    /** Ej: "Lunes 6 de julio de 2026" — para el detalle completo del evento. */
    public String getFechaLabel() {
        String diaSemana = capitalizar(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES));
        String mes = fecha.getMonth().getDisplayName(TextStyle.FULL, ES);
        return diaSemana + " " + fecha.getDayOfMonth() + " de " + mes + " de " + fecha.getYear();
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
