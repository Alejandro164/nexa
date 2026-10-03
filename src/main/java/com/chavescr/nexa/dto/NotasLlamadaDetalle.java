package com.chavescr.nexa.dto;

/** Una llamada de atención del estudiante en el período consultado. */
public class NotasLlamadaDetalle {

    private final String fecha;
    private final String motivo;
    private final String descripcion;
    private final String docente;
    private final String estado;
    private final String estadoClase;

    public NotasLlamadaDetalle(String fecha, String motivo, String descripcion, String docente, String estado,
            String estadoClase) {
        this.fecha = fecha;
        this.motivo = motivo;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.docente = docente == null ? "" : docente;
        this.estado = estado;
        this.estadoClase = estadoClase;
    }

    public String getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDocente() {
        return docente;
    }

    public String getEstado() {
        return estado;
    }

    public String getEstadoClase() {
        return estadoClase;
    }
}
