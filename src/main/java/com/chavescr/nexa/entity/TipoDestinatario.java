package com.chavescr.nexa.entity;

/** A quién se dirige un oficio dentro de la dirección: grupos por rol, familias de una sección o una persona. */
public enum TipoDestinatario {

    COMUNIDAD("Toda la comunidad", "Docentes, familias y estudiantes"),
    DOCENTES("Personal docente", "Docentes de la dirección"),
    FAMILIAS("Todas las familias", "Madres, padres y encargados"),
    ESTUDIANTES("Todos los estudiantes", "Estudiantado de la dirección"),
    FAMILIAS_SECCION("Familias de una sección", "Familias de los estudiantes de la sección"),
    PERSONA("Persona", "Una persona en particular");

    private final String etiqueta;
    private final String descripcion;

    TipoDestinatario(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Los grupos que se eligen con una casilla (no llevan sección ni persona). */
    public boolean esGrupoSimple() {
        return this != FAMILIAS_SECCION && this != PERSONA;
    }
}
