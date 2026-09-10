package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;

public class PanelIncidenteConducta {

    private final List<PeriodoAcademico> periodos;
    private final List<Integer> grados;
    private final List<NivelAcademico> secciones;
    private final List<FilaIncidenteConducta> filas;
    private final Long periodoId;
    private final Integer grado;
    private final Long nivelId;
    private final String avisoPeriodo;

    public PanelIncidenteConducta(List<PeriodoAcademico> periodos, List<Integer> grados,
            List<NivelAcademico> secciones, List<FilaIncidenteConducta> filas, Long periodoId, Integer grado,
            Long nivelId, String avisoPeriodo) {
        this.periodos = periodos;
        this.grados = grados;
        this.secciones = secciones;
        this.filas = filas;
        this.periodoId = periodoId;
        this.grado = grado;
        this.nivelId = nivelId;
        this.avisoPeriodo = avisoPeriodo;
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

    public List<FilaIncidenteConducta> getFilas() {
        return filas;
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
}
