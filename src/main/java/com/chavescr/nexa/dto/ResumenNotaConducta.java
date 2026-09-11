package com.chavescr.nexa.dto;

public class ResumenNotaConducta {

    private final int promedio;
    private final long llamadas;
    private final long boletas;
    private final int totalEstudiantes;

    public ResumenNotaConducta(int promedio, long llamadas, long boletas, int totalEstudiantes) {
        this.promedio = promedio;
        this.llamadas = llamadas;
        this.boletas = boletas;
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

    public int getTotalEstudiantes() {
        return totalEstudiantes;
    }
}
