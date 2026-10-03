package com.chavescr.nexa.dto;

/** Un rubro del detalle: una tarea, prueba, proyecto, trabajo cotidiano o lección. */
public class NotasRubroDetalle {

    private final String titulo;
    private final String descripcion;
    private final String fecha;
    private final String peso;
    private final Integer nota;
    private final String puntos;
    private final String observacion;
    private final boolean entra;
    private final String estado;
    private final String estadoClase;

    public NotasRubroDetalle(String titulo, String descripcion, String fecha, String peso, Integer nota,
            String puntos, String observacion, boolean entra, String estado, String estadoClase) {
        this.titulo = titulo == null || titulo.isBlank() ? "Sin título" : titulo;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.fecha = fecha == null ? "" : fecha;
        this.peso = peso == null ? "" : peso;
        this.nota = nota;
        this.puntos = puntos == null ? "" : puntos;
        this.observacion = observacion == null ? "" : observacion;
        this.entra = entra;
        this.estado = estado == null ? "" : estado;
        this.estadoClase = estadoClase == null ? "" : estadoClase;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getFecha() {
        return fecha;
    }

    public String getPeso() {
        return peso;
    }

    public Integer getNota() {
        return nota;
    }

    public String getPuntos() {
        return puntos;
    }

    public String getObservacion() {
        return observacion;
    }

    public boolean isEntra() {
        return entra;
    }

    public String getEstado() {
        return estado;
    }

    public String getEstadoClase() {
        return estadoClase;
    }
}
