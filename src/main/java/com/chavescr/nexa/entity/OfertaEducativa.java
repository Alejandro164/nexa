package com.chavescr.nexa.entity;

public enum OfertaEducativa {
    PREESCOLAR("Preescolar"),
    PRIMARIA("Primaria"),
    SECUNDARIA("Secundaria");

    private final String etiqueta;

    OfertaEducativa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
