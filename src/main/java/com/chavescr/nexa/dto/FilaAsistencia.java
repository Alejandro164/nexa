package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.Usuario;

public class FilaAsistencia {

    private final Usuario estudiante;
    private final EstadoAsistencia estado;
    private final String observaciones;

    public FilaAsistencia(Usuario estudiante, EstadoAsistencia estado, String observaciones) {
        this.estudiante = estudiante;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public EstadoAsistencia getEstado() {
        return estado;
    }

    public String getObservaciones() {
        return observaciones;
    }
}
