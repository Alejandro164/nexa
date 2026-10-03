package com.chavescr.nexa.dto;

import java.util.List;

/** Detalle de una materia y un componente: los rubros que forman la nota de la celda. */
public class NotasMateriaDetalle {

    private final String materia;
    private final String componente;
    private final boolean asistencia;
    private final boolean activo;
    private final Integer promedio;
    private final int pesoNota;
    private final int total;
    private final int entran;
    private final String leyenda;
    private final boolean muestraPuntos;
    private final List<NotasRubroDetalle> rubros;

    public NotasMateriaDetalle(String materia, String componente, boolean asistencia, boolean activo,
            Integer promedio, int pesoNota, int total, int entran, String leyenda, boolean muestraPuntos,
            List<NotasRubroDetalle> rubros) {
        this.materia = materia;
        this.componente = componente;
        this.asistencia = asistencia;
        this.activo = activo;
        this.promedio = promedio;
        this.pesoNota = pesoNota;
        this.total = total;
        this.entran = entran;
        this.leyenda = leyenda == null ? "" : leyenda;
        this.muestraPuntos = muestraPuntos;
        this.rubros = rubros == null ? List.of() : List.copyOf(rubros);
    }

    public String getMateria() {
        return materia;
    }

    public String getComponente() {
        return componente;
    }

    public boolean isAsistencia() {
        return asistencia;
    }

    public boolean isActivo() {
        return activo;
    }

    public Integer getPromedio() {
        return promedio;
    }

    public int getPesoNota() {
        return pesoNota;
    }

    public int getTotal() {
        return total;
    }

    public int getEntran() {
        return entran;
    }

    public String getLeyenda() {
        return leyenda;
    }

    public boolean isMuestraPuntos() {
        return muestraPuntos;
    }

    public List<NotasRubroDetalle> getRubros() {
        return rubros;
    }
}
