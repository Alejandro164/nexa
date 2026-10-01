package com.chavescr.nexa.dto;

/** Una lección en la que el estudiante faltó o llegó tarde. */
public class NotasAusenciaDetalle {

    private final String fecha;
    private final String materia;
    private final int leccion;
    private final String tipo;
    private final String clase;
    private final String observacion;
    private final String profesor;

    public NotasAusenciaDetalle(String fecha, String materia, int leccion, String tipo, String clase,
            String observacion, String profesor) {
        this.fecha = fecha;
        this.materia = materia;
        this.leccion = leccion;
        this.tipo = tipo;
        this.clase = clase;
        this.observacion = observacion == null ? "" : observacion;
        this.profesor = profesor == null ? "" : profesor;
    }

    public String getFecha() {
        return fecha;
    }

    public String getMateria() {
        return materia;
    }

    public int getLeccion() {
        return leccion;
    }

    public String getTipo() {
        return tipo;
    }

    public String getClase() {
        return clase;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getProfesor() {
        return profesor;
    }
}
