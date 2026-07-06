package com.chavescr.nexa.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Representa una celda de día dentro de una vista del calendario (mes, semana, día o agenda). */
public class DiaCalendarioDTO {

    private static final Locale ES = new Locale("es");

    private final LocalDate fecha;
    private final boolean otroMes;
    private final boolean hoy;
    private final List<EventoCalendarioDTO> eventos;

    public DiaCalendarioDTO(LocalDate fecha, boolean otroMes, boolean hoy, List<EventoCalendarioDTO> eventos) {
        this.fecha = fecha;
        this.otroMes = otroMes;
        this.hoy = hoy;
        this.eventos = eventos;
    }

    public LocalDate getFecha() { return fecha; }
    public boolean isOtroMes() { return otroMes; }
    public boolean isHoy() { return hoy; }
    public List<EventoCalendarioDTO> getEventos() { return eventos; }

    public int getDiaMes() { return fecha.getDayOfMonth(); }

    /** Eventos de este día excluyendo los institucionales de varios días (esos se muestran como banda arriba). */
    public List<EventoCalendarioDTO> getEventosSinBandas() {
        return eventos.stream().filter(ev -> !ev.isMultiDia()).toList();
    }

    /** Ej: "Lunes 6" — para encabezados de columna en la vista de semana. */
    public String getEtiquetaCorta() {
        return capitalizar(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES)) + " " + fecha.getDayOfMonth();
    }

    /** Ej: "Lunes 6 julio" — para encabezados de fecha en la vista de agenda. */
    public String getEtiquetaLarga() {
        return capitalizar(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES)) + " " + fecha.getDayOfMonth()
                + " " + fecha.getMonth().getDisplayName(TextStyle.FULL, ES);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
