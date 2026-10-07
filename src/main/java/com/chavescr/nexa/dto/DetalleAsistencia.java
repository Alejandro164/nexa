package com.chavescr.nexa.dto;

import java.util.List;

/** Tardías y ausencias de un estudiante, un bloque por período del año lectivo. */
public class DetalleAsistencia {

    private final String nombre;
    private final String iniciales;
    private final String meta;
    private final List<Periodo> periodos;

    public DetalleAsistencia(String nombre, String iniciales, String meta, List<Periodo> periodos) {
        this.nombre = nombre;
        this.iniciales = iniciales;
        this.meta = meta;
        this.periodos = periodos;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getMeta() {
        return meta;
    }

    public List<Periodo> getPeriodos() {
        return periodos;
    }

    public static class Periodo {
        private final Long id;
        private final String codigo;
        private final String rango;
        private final List<Cuadro> cuadros;
        private final List<Fila> filas;
        private final boolean actual;

        public Periodo(Long id, String codigo, String rango, List<Cuadro> cuadros, List<Fila> filas, boolean actual) {
            this.id = id;
            this.codigo = codigo;
            this.rango = rango;
            this.cuadros = cuadros;
            this.filas = filas;
            this.actual = actual;
        }

        public Long getId() {
            return id;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getRango() {
            return rango;
        }

        public List<Cuadro> getCuadros() {
            return cuadros;
        }

        public List<Fila> getFilas() {
            return filas;
        }

        public boolean isActual() {
            return actual;
        }
    }

    public static class Cuadro {
        private final String clase;
        private final String etiqueta;
        private final int cantidad;
        private final int puntos;

        public Cuadro(String clase, String etiqueta, int cantidad, int puntos) {
            this.clase = clase;
            this.etiqueta = etiqueta;
            this.cantidad = cantidad;
            this.puntos = puntos;
        }

        public String getClase() {
            return clase;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public int getCantidad() {
            return cantidad;
        }

        public int getPuntos() {
            return puntos;
        }

        public boolean isCero() {
            return puntos == 0;
        }

        public String getPuntosTexto() {
            return puntos == 0 ? "0 puntos" : "−" + puntos + " puntos";
        }
    }

    public static class Fila {
        private final String fecha;
        private final String materia;
        private final String leccion;
        private final String estado;
        private final String estadoClase;
        private final String docente;
        private final String detalle;

        public Fila(String fecha, String materia, String leccion, String estado, String estadoClase, String docente,
                String detalle) {
            this.fecha = fecha;
            this.materia = materia;
            this.leccion = leccion;
            this.estado = estado;
            this.estadoClase = estadoClase;
            this.docente = docente;
            this.detalle = detalle;
        }

        public String getFecha() {
            return fecha;
        }

        public String getMateria() {
            return materia;
        }

        public String getLeccion() {
            return leccion;
        }

        public String getEstado() {
            return estado;
        }

        public String getEstadoClase() {
            return estadoClase;
        }

        public String getDocente() {
            return docente;
        }

        public String getDetalle() {
            return detalle;
        }
    }
}
