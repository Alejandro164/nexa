package com.chavescr.nexa.dto;

import java.util.List;

/** Escala lista para pintar: tramos de la nota más alta a la más baja, cada uno con su tono. */
public record VistaEscala(List<Tramo> tramos) {

    public record Tramo(String nombre, int desde, String tono) {
    }

    /** Tramo donde cae la nota; debe llegar ya redondeada tal como se muestra. */
    public Tramo tramo(Number nota) {
        for (Tramo tramo : tramos) {
            if (nota.doubleValue() >= tramo.desde()) {
                return tramo;
            }
        }
        return tramos.get(tramos.size() - 1);
    }

    /** Tono del tramo donde cae la nota; debe llegar ya redondeada tal como se muestra. */
    public String tono(Number nota) {
        return tramo(nota).tono();
    }
}
