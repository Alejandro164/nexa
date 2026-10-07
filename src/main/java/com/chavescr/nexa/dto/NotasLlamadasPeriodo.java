package com.chavescr.nexa.dto;

import java.util.List;

/** Llamadas de atención de un estudiante en un período. */
public class NotasLlamadasPeriodo {

    private final List<NotasLlamadaDetalle> registros;
    private final int pendientes;
    private final int enProceso;
    private final int resueltas;
    private final int apeladas;

    public NotasLlamadasPeriodo(List<NotasLlamadaDetalle> registros) {
        this.registros = registros;
        int pendientes = 0;
        int enProceso = 0;
        int resueltas = 0;
        int apeladas = 0;
        for (NotasLlamadaDetalle registro : registros) {
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
    }

    public List<NotasLlamadaDetalle> getRegistros() {
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

    public int getTotal() {
        return registros.size();
    }
}
