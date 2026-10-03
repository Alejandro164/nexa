package com.chavescr.nexa.dto;

import java.util.Locale;

/** Una materia del desglose: nota de cada componente y el promedio del período. */
public class NotasDesgloseFila {

    private final String nombre;
    private final String docente;
    private final Integer cotidiano;
    private final Integer tareas;
    private final Integer proyecto;
    private final Integer pruebas;
    private final Integer asistencia;
    private final Double promedio;

    public NotasDesgloseFila(String nombre, String docente, Integer cotidiano, Integer tareas, Integer proyecto,
            Integer pruebas, Integer asistencia, Double promedio) {
        this.nombre = nombre;
        this.docente = docente == null ? "" : docente;
        this.cotidiano = cotidiano;
        this.tareas = tareas;
        this.proyecto = proyecto;
        this.pruebas = pruebas;
        this.asistencia = asistencia;
        this.promedio = promedio;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDocente() {
        return docente;
    }

    public Integer getCotidiano() {
        return cotidiano;
    }

    public Integer getTareas() {
        return tareas;
    }

    public Integer getProyecto() {
        return proyecto;
    }

    public Integer getPruebas() {
        return pruebas;
    }

    public Integer getAsistencia() {
        return asistencia;
    }

    public Double getPromedio() {
        return promedio;
    }

    public String getPromedioTexto() {
        return promedio == null ? null : String.format(Locale.US, "%.1f", promedio);
    }
}
