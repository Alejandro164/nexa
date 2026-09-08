package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.Usuario;

/** Resumen de carga laboral de un docente en un período, derivado de su horario asignado. */
public class CargaLaboralDocenteDTO {

    private Usuario docente;
    private int totalLecciones;
    private List<Asignacion> asignaciones;

    public CargaLaboralDocenteDTO(Usuario docente, int totalLecciones, List<Asignacion> asignaciones) {
        this.docente = docente;
        this.totalLecciones = totalLecciones;
        this.asignaciones = asignaciones;
    }

    public Usuario getDocente() {
        return docente;
    }

    public int getTotalLecciones() {
        return totalLecciones;
    }

    public List<Asignacion> getAsignaciones() {
        return asignaciones;
    }

    /** Cuántas lecciones semanales imparte este docente en una materia+sección específica. */
    public static class Asignacion {
        private final String materiaNombre;
        private final String nivelNombre;
        private final long lecciones;

        public Asignacion(String materiaNombre, String nivelNombre, long lecciones) {
            this.materiaNombre = materiaNombre;
            this.nivelNombre = nivelNombre;
            this.lecciones = lecciones;
        }

        public String getMateriaNombre() {
            return materiaNombre;
        }

        public String getNivelNombre() {
            return nivelNombre;
        }

        public long getLecciones() {
            return lecciones;
        }
    }
}
