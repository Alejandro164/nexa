package com.chavescr.nexa.dto;

import java.util.List;

/** Boletas de un estudiante en un período. */
public class NotasBoletasPeriodo {

    private final List<NotasBoletaDetalle> registros;
    private final int pendientes;
    private final int enProceso;
    private final int resueltas;
    private final int apeladas;
    private final int puntos;

    public NotasBoletasPeriodo(List<NotasBoletaDetalle> registros) {
        this.registros = registros;
        int pendientes = 0;
        int enProceso = 0;
        int resueltas = 0;
        int apeladas = 0;
        int puntos = 0;
        for (NotasBoletaDetalle registro : registros) {
            puntos += registro.getPuntos();
            switch (registro.getEstadoClase()) {
                case "pendiente" -> pendientes++;
                case "enproceso" -> enProceso++;
                case "resuelto" -> resueltas++;
                case "apelado" -> apeladas++;
                default -> {
                }
            }
        }
        this.pendientes = pendientes;
        this.enProceso = enProceso;
        this.resueltas = resueltas;
        this.apeladas = apeladas;
        this.puntos = puntos;
    }

    public List<NotasBoletaDetalle> getRegistros() {
        return registros;
    }

    public int getPendientes() {
        return pendientes;
    }

    public int getEnProceso() {
        return enProceso;
    }

    public int getResueltas() {
        return resueltas;
    }

    public int getApeladas() {
        return apeladas;
    }

    public int getPuntos() {
        return puntos;
    }

    public int getTotal() {
        return registros.size();
    }
}
