package com.chavescr.nexa.dto;

import java.util.List;

/**
 * Consulta de notas recortada al rol: años lectivos, períodos de esos años,
 * grados, secciones y la calificación de cada materia.
 */
public class NotasCatalogo {

    private final boolean elegirAnio;
    private final Integer anio;
    private final String anioEtiqueta;
    private final String aviso;
    private final List<AnioOpcion> anios;
    private final List<PeriodoOpcion> periodos;
    private final List<NivelOpcion> niveles;
    private final List<EstudianteOpcion> estudiantes;

    public NotasCatalogo(boolean elegirAnio, Integer anio, String anioEtiqueta, String aviso,
            List<AnioOpcion> anios, List<PeriodoOpcion> periodos, List<NivelOpcion> niveles,
            List<EstudianteOpcion> estudiantes) {
        this.elegirAnio = elegirAnio;
        this.anio = anio;
        this.anioEtiqueta = anioEtiqueta;
        this.aviso = aviso;
        this.anios = anios;
        this.periodos = periodos;
        this.niveles = niveles;
        this.estudiantes = estudiantes;
    }

    public boolean isElegirAnio() {
        return elegirAnio;
    }

    public Integer getAnio() {
        return anio;
    }

    public String getAnioEtiqueta() {
        return anioEtiqueta;
    }

    public String getAviso() {
        return aviso;
    }

    public List<AnioOpcion> getAnios() {
        return anios;
    }

    public List<PeriodoOpcion> getPeriodos() {
        return periodos;
    }

    public List<NivelOpcion> getNiveles() {
        return niveles;
    }

    public List<EstudianteOpcion> getEstudiantes() {
        return estudiantes;
    }

    public static class AnioOpcion {
        private final Integer anio;
        private final String etiqueta;

        public AnioOpcion(Integer anio, String etiqueta) {
            this.anio = anio;
            this.etiqueta = etiqueta;
        }

        public Integer getAnio() {
            return anio;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    public static class PeriodoOpcion {
        private final Long id;
        private final Integer anio;
        private final String etiqueta;
        private final String titulo;

        public PeriodoOpcion(Long id, Integer anio, String etiqueta, String titulo) {
            this.id = id;
            this.anio = anio;
            this.etiqueta = etiqueta;
            this.titulo = titulo;
        }

        public Long getId() {
            return id;
        }

        public Integer getAnio() {
            return anio;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getTitulo() {
            return titulo;
        }
    }

    public static class NivelOpcion {
        private final Long id;
        private final Integer grado;
        private final String seccion;
        private final String etiqueta;

        public NivelOpcion(Long id, Integer grado, String seccion, String etiqueta) {
            this.id = id;
            this.grado = grado;
            this.seccion = seccion;
            this.etiqueta = etiqueta;
        }

        public Long getId() {
            return id;
        }

        public Integer getGrado() {
            return grado;
        }

        public String getSeccion() {
            return seccion;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    public static class EstudianteOpcion {
        private final Long id;
        private final String nombre;
        private final String cedula;
        private final Integer grado;
        private final String seccion;
        private final Long nivelId;
        private final String iniciales;
        private final String color;
        private final List<MateriaNota> materias;

        public EstudianteOpcion(Long id, String nombre, String cedula, Integer grado, String seccion,
                Long nivelId, String iniciales, String color, List<MateriaNota> materias) {
            this.id = id;
            this.nombre = nombre;
            this.cedula = cedula;
            this.grado = grado;
            this.seccion = seccion;
            this.nivelId = nivelId;
            this.iniciales = iniciales;
            this.color = color;
            this.materias = materias;
        }

        public Long getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getCedula() {
            return cedula;
        }

        public Integer getGrado() {
            return grado;
        }

        public String getSeccion() {
            return seccion;
        }

        public Long getNivelId() {
            return nivelId;
        }

        public String getIniciales() {
            return iniciales;
        }

        public String getColor() {
            return color;
        }

        public List<MateriaNota> getMaterias() {
            return materias;
        }
    }

    public static class MateriaNota {
        private final Long id;
        private final String nombre;
        private final String docente;
        private final List<Long> periodoIds;
        private final List<NotaPeriodo> notas;

        public MateriaNota(Long id, String nombre, String docente, List<Long> periodoIds, List<NotaPeriodo> notas) {
            this.id = id;
            this.nombre = nombre;
            this.docente = docente;
            this.periodoIds = periodoIds;
            this.notas = notas;
        }

        public Long getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getDocente() {
            return docente;
        }

        public List<Long> getPeriodoIds() {
            return periodoIds;
        }

        public List<NotaPeriodo> getNotas() {
            return notas;
        }
    }

    /** Calificación de una materia en un período: porcentaje ponderado y puntos. */
    public static class NotaPeriodo {
        private final Long periodoId;
        private final Double porcentaje;
        private final Integer puntos;
        private final Integer puntosTotales;

        public NotaPeriodo(Long periodoId, Double porcentaje, Integer puntos, Integer puntosTotales) {
            this.periodoId = periodoId;
            this.porcentaje = porcentaje;
            this.puntos = puntos;
            this.puntosTotales = puntosTotales;
        }

        public Long getPeriodoId() {
            return periodoId;
        }

        public Double getPorcentaje() {
            return porcentaje;
        }

        public Integer getPuntos() {
            return puntos;
        }

        public Integer getPuntosTotales() {
            return puntosTotales;
        }
    }
}
