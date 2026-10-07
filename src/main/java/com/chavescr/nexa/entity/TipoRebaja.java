package com.chavescr.nexa.entity;

public enum TipoRebaja {
    LLAMADA("llamada", "Llamada de atención", 1, 5),
    TARDIA_INJUSTIFICADA("tardia-injustificada", "Tardía injustificada", 1, 2),
    TARDIA_JUSTIFICADA("tardia-justificada", "Tardía justificada", 1, 0),
    AUSENCIA_INJUSTIFICADA("ausencia-injustificada", "Ausencia injustificada", 1, 5),
    AUSENCIA_JUSTIFICADA("ausencia-justificada", "Ausencia justificada", 1, 0);

    private final String id;
    private final String nombre;
    private final int cada;
    private final int puntos;

    TipoRebaja(String id, String nombre, int cada, int puntos) {
        this.id = id;
        this.nombre = nombre;
        this.cada = cada;
        this.puntos = puntos;
    }

    public String id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public int cada() {
        return cada;
    }

    public int puntos() {
        return puntos;
    }

    public static TipoRebaja porId(String id) {
        for (TipoRebaja tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        return null;
    }
}
