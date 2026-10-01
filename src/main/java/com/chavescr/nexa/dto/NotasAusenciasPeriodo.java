package com.chavescr.nexa.dto;

import java.util.List;

/** Ausencias y tardías de un estudiante en un período, una fila por lección. */
public class NotasAusenciasPeriodo {

    private final List<NotasAusenciaDetalle> registros;
    private final int justificadas;
    private final int injustificadas;
    private final int tardiasJustificadas;
    private final int tardiasInjustificadas;

    public NotasAusenciasPeriodo(List<NotasAusenciaDetalle> registros) {
        this.registros = registros;
        int justificadas = 0;
        int injustificadas = 0;
        int tardiasJustificadas = 0;
        int tardiasInjustificadas = 0;
        for (NotasAusenciaDetalle registro : registros) {
            switch (registro.getClase()) {
                case "justificada" -> justificadas++;
                case "injustificada" -> injustificadas++;
                case "tardia-justificada" -> tardiasJustificadas++;
                case "tardia" -> tardiasInjustificadas++;
                default -> {
                }
            }
        }
        this.justificadas = justificadas;
        this.injustificadas = injustificadas;
        this.tardiasJustificadas = tardiasJustificadas;
        this.tardiasInjustificadas = tardiasInjustificadas;
    }

    public List<NotasAusenciaDetalle> getRegistros() {
        return registros;
    }

    public int getJustificadas() {
        return justificadas;
    }

    public int getInjustificadas() {
        return injustificadas;
    }

    public int getTardiasJustificadas() {
        return tardiasJustificadas;
    }

    public int getTardiasInjustificadas() {
        return tardiasInjustificadas;
    }

    public int getTotal() {
        return registros.size();
    }
}
