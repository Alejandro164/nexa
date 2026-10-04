package com.chavescr.nexa.service;

import java.util.EnumMap;
import java.util.Map;

import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.TipoRebaja;

/**
 * Puntos que restan de la nota de conducta. Un grupo completo ({@code cantidad / cada})
 * baja {@code puntos}; lo que no llega a completar un grupo no rebaja. Cero puntos
 * apaga ese registro. Las ausencias y tardías solo entran cuando la institución
 * las rebaja en la conducta.
 */
public final class CalculoRebaja {

    public static final int NOTA_INICIAL = 100;

    private final Map<TipoRebaja, int[]> reglas;
    private final boolean asistenciaEnConducta;

    public CalculoRebaja(Map<TipoRebaja, int[]> reglas, boolean asistenciaEnConducta) {
        this.reglas = new EnumMap<>(TipoRebaja.class);
        if (reglas != null) {
            reglas.forEach((tipo, valores) -> {
                if (tipo != null && valores != null && valores.length >= 2) {
                    this.reglas.put(tipo, new int[] { valores[0], valores[1] });
                }
            });
        }
        this.asistenciaEnConducta = asistenciaEnConducta;
    }

    public boolean asistenciaRebajaConducta() {
        return asistenciaEnConducta;
    }

    /** [ausencia justificada, injustificada, tardía justificada, injustificada]. Presente no entra. */
    public static int indice(EstadoAsistencia estado) {
        if (estado == null) {
            return -1;
        }
        return switch (estado) {
            case JUSTIFICADA -> 0;
            case AUSENTE -> 1;
            case TARDIA_JUSTIFICADA -> 2;
            case TARDIA -> 3;
            case PRESENTE -> -1;
        };
    }

    public static void sumar(int[] conteo, EstadoAsistencia estado, int cantidad) {
        int indice = indice(estado);
        if (conteo == null || indice < 0 || cantidad <= 0) {
            return;
        }
        conteo[indice] += cantidad;
    }

    public int puntos(TipoRebaja tipo, int cantidad) {
        int[] regla = reglaDe(tipo);
        if (regla == null || cantidad <= 0) {
            return 0;
        }
        return (cantidad / regla[0]) * regla[1];
    }

    /**
     * Puntos de cada registro, en el orden en que ocurrió. El que completa el grupo
     * carga los puntos; los demás quedan en cero. La suma es {@link #puntos}.
     */
    public int[] puntosPorRegistro(TipoRebaja tipo, int cantidad) {
        int[] marcas = new int[Math.max(0, cantidad)];
        int[] regla = reglaDe(tipo);
        if (regla == null || cantidad <= 0) {
            return marcas;
        }
        for (int i = 0; i < cantidad; i++) {
            int grupos = (i + 1) / regla[0] - i / regla[0];
            marcas[i] = grupos * regla[1];
        }
        return marcas;
    }

    private int[] reglaDe(TipoRebaja tipo) {
        if (tipo == null || (tipo != TipoRebaja.LLAMADA && !asistenciaEnConducta)) {
            return null;
        }
        int[] regla = reglas.get(tipo);
        if (regla == null || regla[0] < 1 || regla[1] <= 0) {
            return null;
        }
        return regla;
    }

    public int puntosAsistencia(int[] conteo) {
        if (!asistenciaEnConducta) {
            return 0;
        }
        return rebajaAsistencia(conteo);
    }

    /** Puntos que bajan las ausencias y tardías, sin importar si van a la conducta o al componente. */
    public int rebajaAsistencia(int[] conteo) {
        if (conteo == null || conteo.length < 4) {
            return 0;
        }
        return bruto(TipoRebaja.AUSENCIA_JUSTIFICADA, conteo[0])
                + bruto(TipoRebaja.AUSENCIA_INJUSTIFICADA, conteo[1])
                + bruto(TipoRebaja.TARDIA_JUSTIFICADA, conteo[2])
                + bruto(TipoRebaja.TARDIA_INJUSTIFICADA, conteo[3]);
    }

    /** La asistencia del componente parte de 100 y resta {@link #rebajaAsistencia}. */
    public int notaAsistencia(int[] conteo) {
        return Math.max(0, NOTA_INICIAL - rebajaAsistencia(conteo));
    }

    private int bruto(TipoRebaja tipo, int cantidad) {
        if (tipo == null || cantidad <= 0) {
            return 0;
        }
        int[] regla = reglas.get(tipo);
        if (regla == null || regla[0] < 1 || regla[1] <= 0) {
            return 0;
        }
        return (cantidad / regla[0]) * regla[1];
    }

    public int nota(int puntosBoletas, int[] conteo) {
        return nota(puntosBoletas, 0, conteo);
    }

    public int nota(int puntosBoletas, int llamadas, int[] conteo) {
        int descuento = Math.max(0, puntosBoletas) + puntos(TipoRebaja.LLAMADA, llamadas) + puntosAsistencia(conteo);
        return Math.max(0, NOTA_INICIAL - descuento);
    }
}
