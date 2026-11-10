package com.chavescr.nexa.entity;

/**
 * Ciclos del sistema educativo costarricense. Cada sección (nivel académico) pertenece a uno y cada ciclo tiene
 * su propia escala de notas en la dirección.
 */
public enum CicloEducativo {
    PRIMER_CICLO("Primer Ciclo", 1, 3),
    SEGUNDO_CICLO("Segundo Ciclo", 4, 6),
    TERCER_CICLO("Tercer Ciclo", 7, 9),
    CUARTO_CICLO("Cuarto Ciclo", 10, 12);

    private final String etiqueta;
    private final int gradoDesde;
    private final int gradoHasta;

    CicloEducativo(String etiqueta, int gradoDesde, int gradoHasta) {
        this.etiqueta = etiqueta;
        this.gradoDesde = gradoDesde;
        this.gradoHasta = gradoHasta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getGradoDesde() {
        return gradoDesde;
    }

    public int getGradoHasta() {
        return gradoHasta;
    }

    /** Ciclo que corresponde a un grado (1.º a 3.º Primer, …, 10.º a 12.º Cuarto); null si el grado no encaja. */
    public static CicloEducativo deGrado(Integer grado) {
        if (grado == null) {
            return null;
        }
        for (CicloEducativo ciclo : values()) {
            if (grado >= ciclo.gradoDesde && grado <= ciclo.gradoHasta) {
                return ciclo;
            }
        }
        return null;
    }
}
