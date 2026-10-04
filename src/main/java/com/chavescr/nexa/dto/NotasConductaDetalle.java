package com.chavescr.nexa.dto;

import java.util.List;

/** Desglose de la nota de conducta: la cuenta del período, o una cuenta por período en el año. */
public class NotasConductaDetalle {

    private final boolean anio;
    private final boolean asistenciaEnConducta;
    private final String nota;
    private final List<Cuenta> cuentas;

    public NotasConductaDetalle(boolean anio, boolean asistenciaEnConducta, String nota, List<Cuenta> cuentas) {
        this.anio = anio;
        this.asistenciaEnConducta = asistenciaEnConducta;
        this.nota = nota;
        this.cuentas = cuentas;
    }

    public boolean isAnio() {
        return anio;
    }

    public boolean isAsistenciaEnConducta() {
        return asistenciaEnConducta;
    }

    public String getNota() {
        return nota;
    }

    public List<Cuenta> getCuentas() {
        return cuentas;
    }

    public static class Cuenta {
        private final String periodo;
        private final List<Linea> lineas;
        private final List<Registro> registros;
        private final int nota;

        public Cuenta(String periodo, List<Linea> lineas, List<Registro> registros, int nota) {
            this.periodo = periodo;
            this.lineas = lineas;
            this.registros = registros;
            this.nota = nota;
        }

        public String getPeriodo() {
            return periodo;
        }

        public List<Linea> getLineas() {
            return lineas;
        }

        public List<Registro> getRegistros() {
            return registros;
        }

        public int getNota() {
            return nota;
        }
    }

    public static class Linea {
        private final String concepto;
        private final int cantidad;
        private final int puntos;

        public Linea(String concepto, int cantidad, int puntos) {
            this.concepto = concepto;
            this.cantidad = cantidad;
            this.puntos = puntos;
        }

        public String getConcepto() {
            return concepto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public int getPuntos() {
            return puntos;
        }
    }

    /** Una ausencia o tardía, con los puntos que carga al completar su grupo. */
    public static class Registro {
        private final String fecha;
        private final String materia;
        private final String tipo;
        private final int puntos;

        public Registro(String fecha, String materia, String tipo, int puntos) {
            this.fecha = fecha;
            this.materia = materia;
            this.tipo = tipo;
            this.puntos = puntos;
        }

        public String getFecha() {
            return fecha;
        }

        public String getMateria() {
            return materia;
        }

        public String getTipo() {
            return tipo;
        }

        public int getPuntos() {
            return puntos;
        }
    }
}
