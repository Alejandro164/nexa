package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.ClaveComponente;

/**
 * Nombres con los que un componente de evaluación muestra sus rubros. Cada componente puede renombrarlos
 * (por ejemplo "Pruebas" → "Exámenes"); los que no se renombran usan el nombre predeterminado.
 */
public class NombresRubros {

    public static final String COTIDIANO = "Cotidiano";
    public static final String TAREAS = "Tareas";
    public static final String PROYECTO = "Proyecto";
    public static final String PRUEBAS = "Pruebas";
    public static final String ASISTENCIA = "Asistencia";

    public static final NombresRubros PREDETERMINADOS =
            new NombresRubros(COTIDIANO, TAREAS, PROYECTO, PRUEBAS, ASISTENCIA);

    private final String cotidiano;
    private final String tareas;
    private final String proyecto;
    private final String pruebas;
    private final String asistencia;

    /** Un nombre null o en blanco toma el predeterminado de ese rubro. */
    public NombresRubros(String cotidiano, String tareas, String proyecto, String pruebas, String asistencia) {
        this.cotidiano = o(cotidiano, COTIDIANO);
        this.tareas = o(tareas, TAREAS);
        this.proyecto = o(proyecto, PROYECTO);
        this.pruebas = o(pruebas, PRUEBAS);
        this.asistencia = o(asistencia, ASISTENCIA);
    }

    private static String o(String nombre, String predeterminado) {
        return nombre == null || nombre.isBlank() ? predeterminado : nombre;
    }

    public static String predeterminado(ClaveComponente clave) {
        return PREDETERMINADOS.de(clave);
    }

    public String getCotidiano() {
        return cotidiano;
    }

    public String getTareas() {
        return tareas;
    }

    public String getProyecto() {
        return proyecto;
    }

    public String getPruebas() {
        return pruebas;
    }

    public String getAsistencia() {
        return asistencia;
    }

    public String de(ClaveComponente clave) {
        if (clave == null) {
            return "Componente";
        }
        return switch (clave) {
            case COTIDIANO -> cotidiano;
            case TAREA -> tareas;
            case PROYECTO -> proyecto;
            case EXAMEN -> pruebas;
        };
    }

    /** Para las plantillas: acepta la clave como texto, incluida "ASISTENCIA". */
    public String de(String clave) {
        if ("ASISTENCIA".equals(clave)) {
            return asistencia;
        }
        try {
            return de(ClaveComponente.valueOf(clave));
        } catch (IllegalArgumentException | NullPointerException e) {
            return "Componente";
        }
    }
}
