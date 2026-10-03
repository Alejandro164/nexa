package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.Usuario;

public class FilaPromedio {

    private final Usuario estudiante;
    /** Una nota por tipo activo, en el mismo orden que la lista de columnas. */
    private final List<Double> notas;
    private final Integer asistencia;
    private final Double promedioFinal;

    public FilaPromedio(Usuario estudiante, List<Double> notas, Integer asistencia, Double promedioFinal) {
        this.estudiante = estudiante;
        this.notas = notas;
        this.asistencia = asistencia;
        this.promedioFinal = promedioFinal;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public List<Double> getNotas() {
        return notas;
    }

    public Integer getAsistencia() {
        return asistencia;
    }

    public Double getPromedioFinal() {
        return promedioFinal;
    }
}
