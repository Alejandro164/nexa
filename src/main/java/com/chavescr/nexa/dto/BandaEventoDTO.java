package com.chavescr.nexa.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Banda horizontal de un evento institucional de varios días, posicionada
 * dentro de la grilla semanal del calendario (estilo Outlook/Teams).
 */
public class BandaEventoDTO {

    private static final Locale ES = new Locale("es");

    private final String titulo;
    private final String descripcion;
    private final String enlace;
    private final int columnaInicio;
    private final int columnaFin;
    private final int fila;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;

    public BandaEventoDTO(String titulo, String descripcion, String enlace,
            int columnaInicio, int columnaFin, int fila) {
        this(titulo, descripcion, enlace, columnaInicio, columnaFin, fila, null, null);
    }

    public BandaEventoDTO(String titulo, String descripcion, String enlace,
            int columnaInicio, int columnaFin, int fila, LocalDate fechaInicio, LocalDate fechaFin) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.enlace = enlace;
        this.columnaInicio = columnaInicio;
        this.columnaFin = columnaFin;
        this.fila = fila;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getEnlace() { return enlace; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }

    /** Ej: "6 de julio – 12 de julio de 2026" — rango completo del evento (sin recortar por semana). */
    public String getRangoLabel() {
        if (fechaInicio == null || fechaFin == null) {
            return "";
        }
        String inicio = fechaInicio.getDayOfMonth() + " de "
                + fechaInicio.getMonth().getDisplayName(TextStyle.FULL, ES);
        String fin = fechaFin.getDayOfMonth() + " de "
                + fechaFin.getMonth().getDisplayName(TextStyle.FULL, ES) + " de " + fechaFin.getYear();
        return inicio + " – " + fin;
    }

    /** Columna de inicio dentro de la semana (1 = lunes ... 7 = domingo). */
    public int getColumnaInicio() { return columnaInicio; }

    /** Columna de fin dentro de la semana (1 = lunes ... 7 = domingo), inclusive. */
    public int getColumnaFin() { return columnaFin; }

    /** Fila de apilado dentro de la franja de bandas (0-indexado), para no solaparse con otras bandas. */
    public int getFila() { return fila; }

    /** Valor listo para usar en CSS grid-column: 'inicio / fin+1'. */
    public String getGridColumn() { return columnaInicio + " / " + (columnaFin + 1); }
}
