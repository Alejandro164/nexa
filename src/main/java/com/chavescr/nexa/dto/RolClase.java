package com.chavescr.nexa.dto;

/** Papel del usuario actual dentro de una clase del Classroom. */
public enum RolClase {
    // Imparte la materia en esa sección según el horario: publica, califica y modera
    DOCENTE,
    // Pertenece a la sección: comenta y entrega tareas
    ESTUDIANTE,
    // Admin/Director de la dirección que no imparte la clase: solo lectura
    SUPERVISOR;

    public boolean isDocente() {
        return this == DOCENTE;
    }

    public boolean isEstudiante() {
        return this == ESTUDIANTE;
    }

    public boolean isSupervisor() {
        return this == SUPERVISOR;
    }
}
