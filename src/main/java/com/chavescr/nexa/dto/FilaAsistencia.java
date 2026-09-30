package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.Usuario;

public class FilaAsistencia {

    private final Usuario estudiante;
    private final EstadoAsistencia estado;
    private final String observaciones;
    private final String documentoNombre;

    public FilaAsistencia(Usuario estudiante, EstadoAsistencia estado, String observaciones, String documentoNombre) {
        this.estudiante = estudiante;
        this.estado = estado;
        this.observaciones = observaciones;
        this.documentoNombre = documentoNombre;
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

    public String getDocumentoNombre() {
        return documentoNombre;
    }
}
