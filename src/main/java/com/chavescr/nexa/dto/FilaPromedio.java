package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.Usuario;

public class FilaPromedio {

    private final Usuario estudiante;
    private final Integer cotidiano;
    private final Integer tareas;
    private final Integer proyectos;
    private final Integer examenes;
    private final Integer asistencia;
    private final Double promedioFinal;

    public FilaPromedio(Usuario estudiante, Integer cotidiano, Integer tareas, Integer proyectos,
            Integer examenes, Integer asistencia, Double promedioFinal) {
        this.estudiante = estudiante;
        this.cotidiano = cotidiano;
        this.tareas = tareas;
        this.proyectos = proyectos;
        this.examenes = examenes;
        this.asistencia = asistencia;
        this.promedioFinal = promedioFinal;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public Integer getCotidiano() {
        return cotidiano;
    }

    public Integer getTareas() {
        return tareas;
    }

    public Integer getProyectos() {
        return proyectos;
    }

    public Integer getExamenes() {
        return examenes;
    }

    public Integer getAsistencia() {
        return asistencia;
    }

    public Double getPromedioFinal() {
        return promedioFinal;
    }
}
