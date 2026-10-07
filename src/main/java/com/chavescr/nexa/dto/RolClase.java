package com.chavescr.nexa.dto;

/** Papel del usuario actual dentro de una clase del Classroom. */
public enum RolClase {
    // Imparte la materia en esa sección según el horario: publica, califica y modera
    DOCENTE,
    // Pertenece a la sección: comenta y entrega tareas
    ESTUDIANTE,
    // Admin/Director de la dirección que no imparte la clase: solo lectura
    SUPERVISOR,
    // Padre o madre: ve la clase de sus hijos, sin entregar ni revisar compañeros
    PADRE;

    public boolean isDocente() {
        return this == DOCENTE;
    }

    public boolean isEstudiante() {
        return this == ESTUDIANTE;
    }

    public boolean isSupervisor() {
        return this == SUPERVISOR;
    }

    public boolean isPadre() {
        return this == PADRE;
    }

    /** Docente y supervisión ven las entregas de toda la sección. */
    public boolean isRevision() {
        return this == DOCENTE || this == SUPERVISOR;
    }
}
