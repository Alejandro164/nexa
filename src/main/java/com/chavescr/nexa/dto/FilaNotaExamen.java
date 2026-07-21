package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.Usuario;

public class FilaNotaExamen {

    private final Usuario estudiante;
    private final Integer calificacion;

    public FilaNotaExamen(Usuario estudiante, Integer calificacion) {
        this.estudiante = estudiante;
        this.calificacion = calificacion;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public Integer getCalificacion() {
        return calificacion;
    }
}
