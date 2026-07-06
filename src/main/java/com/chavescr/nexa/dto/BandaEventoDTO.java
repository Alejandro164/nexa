package com.chavescr.nexa.dto;

/**
 * Banda horizontal de un evento institucional de varios días, posicionada
 * dentro de la grilla semanal del calendario (estilo Outlook/Teams).
 */
public class BandaEventoDTO {

    private final String titulo;
    private final String descripcion;
    private final String enlace;
    private final int columnaInicio;
    private final int columnaFin;
    private final int fila;

    public BandaEventoDTO(String titulo, String descripcion, String enlace,
            int columnaInicio, int columnaFin, int fila) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.enlace = enlace;
        this.columnaInicio = columnaInicio;
        this.columnaFin = columnaFin;
        this.fila = fila;
    }

    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getEnlace() { return enlace; }

    /** Columna de inicio dentro de la semana (1 = lunes ... 7 = domingo). */
    public int getColumnaInicio() { return columnaInicio; }

    /** Columna de fin dentro de la semana (1 = lunes ... 7 = domingo), inclusive. */
    public int getColumnaFin() { return columnaFin; }

    /** Fila de apilado dentro de la franja de bandas (0-indexado), para no solaparse con otras bandas. */
    public int getFila() { return fila; }

    /** Valor listo para usar en CSS grid-column: 'inicio / fin+1'. */
    public String getGridColumn() { return columnaInicio + " / " + (columnaFin + 1); }
}
