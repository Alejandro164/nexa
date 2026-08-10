package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.Usuario;

public class FilaEvaluacionCotidiana {

    private final Usuario estudiante;
    private final Integer puntosObtenidos;
    private final Integer calificacion;
    private final String observacion;

    public FilaEvaluacionCotidiana(Usuario estudiante, Integer puntosObtenidos, Integer calificacion, String observacion) {
        this.estudiante = estudiante;
        this.puntosObtenidos = puntosObtenidos;
        this.calificacion = calificacion;
        this.observacion = observacion;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public Integer getPuntosObtenidos() {
        return puntosObtenidos;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public String getObservacion() {
        return observacion;
    }
}
