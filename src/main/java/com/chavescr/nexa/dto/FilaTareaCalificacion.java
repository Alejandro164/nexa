package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.Usuario;

public class FilaTareaCalificacion {

    private final Usuario estudiante;
    private final Integer calificacion;
    private final String observacion;

    public FilaTareaCalificacion(Usuario estudiante, Integer calificacion, String observacion) {
        this.estudiante = estudiante;
        this.calificacion = calificacion;
        this.observacion = observacion;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public String getObservacion() {
        return observacion;
    }
}
