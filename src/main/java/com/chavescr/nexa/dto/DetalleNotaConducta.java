package com.chavescr.nexa.dto;

import java.util.List;

/** Cuenta de conducta de un estudiante, un bloque por período del año. */
public class DetalleNotaConducta {

    private final Long estudianteId;
    private final String nombre;
    private final String iniciales;
    private final String colorAvatar;
    private final String meta;
    private final boolean asistenciaEnConducta;
    private final List<Periodo> periodos;

    public DetalleNotaConducta(Long estudianteId, String nombre, String iniciales, String colorAvatar, String meta,
            boolean asistenciaEnConducta, List<Periodo> periodos) {
        this.estudianteId = estudianteId;
        this.nombre = nombre;
        this.iniciales = iniciales;
        this.colorAvatar = colorAvatar;
        this.meta = meta;
        this.asistenciaEnConducta = asistenciaEnConducta;
        this.periodos = periodos;
    }

    public Long getEstudianteId() {
        return estudianteId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getColorAvatar() {
        return colorAvatar;
    }

    public String getMeta() {
        return meta;
    }

    public boolean isAsistenciaEnConducta() {
        return asistenciaEnConducta;
    }

    public List<Periodo> getPeriodos() {
        return periodos;
    }

    public static class Periodo {
        private final Long id;
        private final String codigo;
        private final String rango;
        private final boolean actual;
        private final String nota;
        private final Concepto base;
        private final List<Concepto> conceptos;

        public Periodo(Long id, String codigo, String rango, boolean actual, String nota, Concepto base,
                List<Concepto> conceptos) {
            this.id = id;
            this.codigo = codigo;
            this.rango = rango;
            this.actual = actual;
            this.nota = nota;
            this.base = base;
            this.conceptos = conceptos;
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

        public boolean isActual() {
            return actual;
        }

        public String getNota() {
            return nota;
        }

        public Concepto getBase() {
            return base;
        }

        public List<Concepto> getConceptos() {
            return conceptos;
        }
    }

    public static class Concepto {
        private final String clave;
        private final String etiqueta;
        private final String cantidad;
        private final boolean cero;
        private final String puntosTexto;
        private final List<Registro> registros;

        public Concepto(String clave, String etiqueta, String cantidad, boolean cero, String puntosTexto,
                List<Registro> registros) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.cantidad = cantidad;
            this.cero = cero;
            this.puntosTexto = puntosTexto;
            this.registros = registros;
        }

        public String getClave() {
            return clave;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getCantidad() {
            return cantidad;
        }

        public boolean isCero() {
            return cero;
        }

        public String getPuntosTexto() {
            return puntosTexto;
        }

        public List<Registro> getRegistros() {
            return registros;
        }
    }

    public static class Registro {
        private final String fecha;
        private final String materia;
        private final String leccion;
        private final String profesor;
        private final String detalle;
        private final boolean cero;
        private final String puntosTexto;

        public Registro(String fecha, String materia, String leccion, String profesor, String detalle, boolean cero,
                String puntosTexto) {
            this.fecha = fecha;
            this.materia = materia;
            this.leccion = leccion;
            this.profesor = profesor;
            this.detalle = detalle;
            this.cero = cero;
            this.puntosTexto = puntosTexto;
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

        public String getProfesor() {
            return profesor;
        }

        public String getDetalle() {
            return detalle;
        }

        public boolean isCero() {
            return cero;
        }

        public String getPuntosTexto() {
            return puntosTexto;
        }
    }
}
