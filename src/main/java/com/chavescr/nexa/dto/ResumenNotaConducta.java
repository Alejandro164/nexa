package com.chavescr.nexa.dto;

public class ResumenNotaConducta {

    private final int promedio;
    private final long llamadas;
    private final long boletas;
    private final long amonestacionesPendientes;
    private final int totalEstudiantes;

    public ResumenNotaConducta(int promedio, long llamadas, long boletas, long amonestacionesPendientes,
            int totalEstudiantes) {
        this.promedio = promedio;
        this.llamadas = llamadas;
        this.boletas = boletas;
        this.amonestacionesPendientes = amonestacionesPendientes;
        this.totalEstudiantes = totalEstudiantes;
    }

    public int getPromedio() {
        return promedio;
    }

    public long getLlamadas() {
        return llamadas;
    }

    public long getBoletas() {
        return boletas;
    }

    public long getAmonestacionesPendientes() {
        return amonestacionesPendientes;
    }

    public int getTotalEstudiantes() {
        return totalEstudiantes;
    }
}
