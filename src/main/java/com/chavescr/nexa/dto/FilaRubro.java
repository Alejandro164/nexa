package com.chavescr.nexa.dto;

import java.time.LocalDate;

public class FilaRubro {

    private final Long id;
    private final String titulo;
    private final LocalDate fecha;
    private final boolean ponderado;
    private final Integer puntosTotales;
    private final Double promedio;
    private final Double peso;
    private final Long evaluados;

    public FilaRubro(Long id, String titulo, LocalDate fecha, boolean ponderado, Integer puntosTotales,
            Double promedio, Double peso, Long evaluados) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
        this.ponderado = ponderado;
        this.puntosTotales = puntosTotales;
        this.promedio = promedio;
        this.peso = peso;
        this.evaluados = evaluados;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public boolean isPonderado() {
        return ponderado;
    }

    public Integer getPuntosTotales() {
        return puntosTotales;
    }

    public Double getPromedio() {
        return promedio;
    }

    public Double getPeso() {
        return peso;
    }

    public Long getEvaluados() {
        return evaluados;
    }
}
