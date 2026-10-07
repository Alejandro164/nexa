package com.chavescr.nexa.dto;

import java.util.List;
import java.util.Locale;

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
        private final int base;
        private final String origenBase;

        public Cuenta(String periodo, List<Linea> lineas, List<Registro> registros, int nota, int base,
                String origenBase) {
            this.periodo = periodo;
            this.lineas = lineas;
            this.registros = registros;
            this.nota = nota;
            this.base = base;
            this.origenBase = origenBase;
        }

        public String getPeriodo() {
            return periodo;
        }

        public int getBase() {
            return base;
        }

        public String getBaseTexto() {
            return String.format(Locale.US, "%.2f", (double) base);
        }

        /** Código del período anterior. Vacío cuando esta cuenta es la primera del año. */
        public String getOrigenBase() {
            return origenBase == null ? "" : origenBase;
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
        private final String clave;

        public Linea(String concepto, int cantidad, int puntos) {
            this(concepto, cantidad, puntos, null);
        }

        public Linea(String concepto, int cantidad, int puntos, String clave) {
            this.concepto = concepto;
            this.cantidad = cantidad;
            this.puntos = puntos;
            this.clave = clave;
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

        public String getClave() {
            return clave == null ? "" : clave;
        }

        /** Hay lecciones que explican esta fila. */
        public boolean isAbre() {
            return clave != null && !clave.isBlank() && cantidad > 0;
        }
    }

    /** Una ausencia o tardía, con los puntos que carga al completar su grupo. */
    public static class Registro {
        private final String fecha;
        private final String materia;
        private final String tipo;
        private final int puntos;
        private final String clave;

        public Registro(String fecha, String materia, String tipo, int puntos) {
            this(fecha, materia, tipo, puntos, null);
        }

        public Registro(String fecha, String materia, String tipo, int puntos, String clave) {
            this.fecha = fecha;
            this.materia = materia;
            this.tipo = tipo;
            this.puntos = puntos;
            this.clave = clave;
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

        public String getClave() {
            return clave == null ? "" : clave;
        }
    }
}
