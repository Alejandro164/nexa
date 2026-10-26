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
    private final LocalDate inicioEvento;
    private final LocalDate finEvento;

    private EventoCalendarioDTO(Tipo tipo, String titulo, String descripcion,
            LocalDate fecha, LocalTime hora, String enlace, LocalDate inicioEvento, LocalDate finEvento) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.hora = hora;
        this.enlace = enlace;
        this.inicioEvento = inicioEvento;
        this.finEvento = finEvento;
    }

    public static EventoCalendarioDTO deTarea(String titulo, String descripcion, LocalDate fecha) {
        return new EventoCalendarioDTO(Tipo.TAREA, titulo, descripcion, fecha, null, null, null, null);
    }

    public static EventoCalendarioDTO deRecordatorio(String titulo, String descripcion, LocalDate fecha, LocalTime hora) {
        return new EventoCalendarioDTO(Tipo.RECORDATORIO, titulo, descripcion, fecha, hora, null, null, null);
    }

    public static EventoCalendarioDTO deInstitucional(String titulo, String descripcion, LocalDate fecha,
            String enlace, LocalDate inicioEvento, LocalDate finEvento) {
        return new EventoCalendarioDTO(Tipo.INSTITUCIONAL, titulo, descripcion, fecha, null, enlace,
                inicioEvento, finEvento);
    }

    public Tipo getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public String getEnlace() { return enlace; }

    /** true si el evento institucional dura varios días (aparece en cada uno de esos días). */
    public boolean isMultiDia() {
        return inicioEvento != null && finEvento != null && finEvento.isAfter(inicioEvento);
    }

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

    /**
     * Ej: "Lunes 6 de julio de 2026" — para el detalle completo del evento. Si dura varios días
     * muestra el rango completo ("Del 6 de julio al 10 de julio de 2026").
     */
    public String getFechaLabel() {
        if (isMultiDia()) {
            return "Del " + inicioEvento.getDayOfMonth() + " de "
                    + inicioEvento.getMonth().getDisplayName(TextStyle.FULL, ES) + " al "
                    + finEvento.getDayOfMonth() + " de " + finEvento.getMonth().getDisplayName(TextStyle.FULL, ES)
                    + " de " + finEvento.getYear();
        }
        String diaSemana = capitalizar(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES));
        String mes = fecha.getMonth().getDisplayName(TextStyle.FULL, ES);
        return diaSemana + " " + fecha.getDayOfMonth() + " de " + mes + " de " + fecha.getYear();
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
