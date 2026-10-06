package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;

/** Lecciones del horario que se imparten con dos o más materias, y el alumno que va a cada una. */
public record VistaSubgrupos(
        List<PeriodoAcademico> periodos,
        List<NivelAcademico> niveles,
        Long periodoId,
        Long nivelId,
        String nivelNombre,
        int totalEstudiantes,
        List<Franja> franjas,
        String abierta,
        boolean configurado) {

    public record Franja(
            String clave,
            String titulo,
            List<String> horarios,
            int total,
            int asignados,
            List<Opcion> opciones,
            List<Alumno> pendientes) {
    }

    public record Opcion(Long leccionId, String nombre, String color, String detalle, List<Alumno> alumnos) {
    }

    public record Alumno(Long id, String nombre, Long leccionId) {
    }
}
