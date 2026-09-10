package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.Usuario;

public class FilaNotaConducta {

    private final Usuario estudiante;
    private final String seccion;
    private final int nota;
    private final String categoria;
    private final String categoriaCss;
    private final String observaciones;
    private final String periodoCodigo;
    private final boolean enviada;
    private final String iniciales;
    private final String colorAvatar;

    public FilaNotaConducta(Usuario estudiante, String seccion, int nota, String categoria, String categoriaCss,
            String observaciones, String periodoCodigo, boolean enviada, String iniciales, String colorAvatar) {
        this.estudiante = estudiante;
        this.seccion = seccion;
        this.nota = nota;
        this.categoria = categoria;
        this.categoriaCss = categoriaCss;
        this.observaciones = observaciones;
        this.periodoCodigo = periodoCodigo;
        this.enviada = enviada;
        this.iniciales = iniciales;
        this.colorAvatar = colorAvatar;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public String getSeccion() {
        return seccion;
    }

    public int getNota() {
        return nota;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getCategoriaCss() {
        return categoriaCss;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public String getPeriodoCodigo() {
        return periodoCodigo;
    }

    public boolean isEnviada() {
        return enviada;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getColorAvatar() {
        return colorAvatar;
    }
}
