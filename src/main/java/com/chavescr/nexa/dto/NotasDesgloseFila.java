package com.chavescr.nexa.dto;

import java.util.Locale;

/** Una materia del desglose: nota de cada componente y el promedio del período. */
public class NotasDesgloseFila {

    private final Long materiaId;
    private final String nombre;
    private final String docente;
    private final Integer cotidiano;
    private final Integer tareas;
    private final Integer proyecto;
    private final Integer pruebas;
    private final Integer asistencia;
    private final Double promedio;

    public NotasDesgloseFila(Long materiaId, String nombre, String docente, Integer cotidiano, Integer tareas,
            Integer proyecto, Integer pruebas, Integer asistencia, Double promedio) {
        this.materiaId = materiaId;
        this.nombre = nombre;
        this.docente = docente == null ? "" : docente;
        this.cotidiano = cotidiano;
        this.tareas = tareas;
        this.proyecto = proyecto;
        this.pruebas = pruebas;
        this.asistencia = asistencia;
        this.promedio = promedio;
    }

    public Long getMateriaId() {
        return materiaId;
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

    public String getCotidianoTexto() {
        return dosDecimales(cotidiano);
    }

    public Integer getTareas() {
        return tareas;
    }

    public String getTareasTexto() {
        return dosDecimales(tareas);
    }

    public Integer getProyecto() {
        return proyecto;
    }

    public String getProyectoTexto() {
        return dosDecimales(proyecto);
    }

    public Integer getPruebas() {
        return pruebas;
    }

    public String getPruebasTexto() {
        return dosDecimales(pruebas);
    }

    public Integer getAsistencia() {
        return asistencia;
    }

    public String getAsistenciaTexto() {
        return dosDecimales(asistencia);
    }

    public Double getPromedio() {
        return promedio;
    }

    public String getPromedioTexto() {
        return dosDecimales(promedio);
    }

    private static String dosDecimales(Number nota) {
        return nota == null ? null : String.format(Locale.US, "%.2f", nota.doubleValue());
    }
}
