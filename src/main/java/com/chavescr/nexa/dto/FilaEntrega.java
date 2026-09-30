package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.ClassroomEntrega;
import com.chavescr.nexa.entity.EstadoEntrega;
import com.chavescr.nexa.entity.Usuario;

/** Fila de la vista de revisión de una tarea: un estudiante de la sección con su entrega y nota. */
public class FilaEntrega {

    private final Usuario estudiante;
    private final ClassroomEntrega entrega;
    private final Integer puntos;
    private final Integer calificacion;

    public FilaEntrega(Usuario estudiante, ClassroomEntrega entrega, Integer puntos, Integer calificacion) {
        this.estudiante = estudiante;
        this.entrega = entrega;
        this.puntos = puntos;
        this.calificacion = calificacion;
    }

    public EstadoEntrega getEstado() {
        return entrega != null ? entrega.getEstado() : EstadoEntrega.ASIGNADA;
    }

    public boolean isTarde() {
        return entrega != null && entrega.isTarde();
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public ClassroomEntrega getEntrega() {
        return entrega;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public Integer getCalificacion() {
        return calificacion;
    }
}
