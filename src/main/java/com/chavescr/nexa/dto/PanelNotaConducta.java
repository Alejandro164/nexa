package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;

public class PanelNotaConducta {

    private final List<PeriodoAcademico> periodos;
    private final List<Integer> grados;
    private final List<NivelAcademico> secciones;
    private final List<FilaNotaConducta> filas;
    private final ResumenNotaConducta resumen;
    private final Long periodoId;
    private final Integer grado;
    private final Long nivelId;
    private final String avisoPeriodo;
    private final VistaEscala escala;

    public PanelNotaConducta(List<PeriodoAcademico> periodos, List<Integer> grados, List<NivelAcademico> secciones,
            List<FilaNotaConducta> filas, ResumenNotaConducta resumen, Long periodoId, Integer grado, Long nivelId,
            String avisoPeriodo, VistaEscala escala) {
        this.periodos = periodos;
        this.grados = grados;
        this.secciones = secciones;
        this.filas = filas;
        this.resumen = resumen;
        this.periodoId = periodoId;
        this.grado = grado;
        this.nivelId = nivelId;
        this.avisoPeriodo = avisoPeriodo;
        this.escala = escala;
    }

    public List<PeriodoAcademico> getPeriodos() {
        return periodos;
    }

    public List<Integer> getGrados() {
        return grados;
    }

    public List<NivelAcademico> getSecciones() {
        return secciones;
    }

    public List<FilaNotaConducta> getFilas() {
        return filas;
    }

    public ResumenNotaConducta getResumen() {
        return resumen;
    }

    public Long getPeriodoId() {
        return periodoId;
    }

    public Integer getGrado() {
        return grado;
    }

    public Long getNivelId() {
        return nivelId;
    }

    public String getAvisoPeriodo() {
        return avisoPeriodo;
    }

    public VistaEscala getEscala() {
        return escala;
    }
}
