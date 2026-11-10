package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.TipoRebaja;

/**
 * Puntos que restan de la nota de conducta y nota del rubro Asistencia.
 * <p>
 * Conducta: un grupo completo ({@code cantidad / cada}) baja {@code puntos}; lo que no llega a completar un grupo
 * no rebaja. Cero puntos apaga ese registro.
 * <p>
 * Asistencia (artículo 37 del Reglamento de Evaluación de los Aprendizajes): sale del porcentaje de ausencias
 * injustificadas sobre las lecciones impartidas en el período, según {@link #TABLA_ASISTENCIA}. Cada tardía
 * injustificada vale {@code 1 / tardiasPorAusencia} ausencias; las justificadas no cuentan.
 */
public final class CalculoRebaja {

    public static final int NOTA_INICIAL = 100;

    /** Tardías injustificadas que forman una ausencia: 2 = cada tardía es media ausencia, 1 = una ausencia. */
    public static final int TARDIAS_POR_AUSENCIA_PREDETERMINADO = 2;

    /**
     * Tabla del artículo 37: {porcentaje de ausencias hasta el que aplica (exclusivo), porcentaje que se asigna de
     * 5}. De 50% o más se asigna 0.
     */
    public static final int[][] TABLA_ASISTENCIA = { { 10, 5 }, { 20, 4 }, { 30, 3 }, { 40, 2 }, { 50, 1 } };

    /** El porcentaje máximo de la tabla, sobre el que se calcula la nota del rubro (5 de 5 = 100). */
    public static final int ASIGNACION_MAXIMA = 5;

    private final Map<TipoRebaja, int[]> reglas;
    private final boolean asistenciaEnConducta;
    private final int tardiasPorAusencia;

    public CalculoRebaja(Map<TipoRebaja, int[]> reglas, boolean asistenciaEnConducta) {
        this(reglas, asistenciaEnConducta, TARDIAS_POR_AUSENCIA_PREDETERMINADO);
    }

    public CalculoRebaja(Map<TipoRebaja, int[]> reglas, boolean asistenciaEnConducta, int tardiasPorAusencia) {
        this.tardiasPorAusencia = tardiasPorAusencia == 1 ? 1 : TARDIAS_POR_AUSENCIA_PREDETERMINADO;
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

    public int getTardiasPorAusencia() {
        return tardiasPorAusencia;
    }

    /** Ausencias injustificadas más las tardías injustificadas convertidas a ausencias. */
    public double ausenciasEquivalentes(int[] conteo) {
        if (conteo == null || conteo.length < 4) {
            return 0;
        }
        return Math.max(0, conteo[1]) + Math.max(0, conteo[3]) / (double) tardiasPorAusencia;
    }

    /** Porcentaje de ausencias injustificadas (con tardías) sobre las lecciones impartidas; 0 sin lecciones. */
    public double porcentajeAusencias(int[] conteo, int lecciones) {
        if (lecciones <= 0) {
            return 0;
        }
        return ausenciasEquivalentes(conteo) * 100.0 / lecciones;
    }

    /** Fila de la tabla del artículo 37 para mostrarla: desde qué % de ausencias, hasta cuál (null = en adelante). */
    public record TramoAsistencia(int desde, Integer hasta, int asignacion, int nota) {
    }

    public static List<TramoAsistencia> tramosAsistencia() {
        List<TramoAsistencia> tramos = new ArrayList<>();
        int desde = 0;
        for (int[] tramo : TABLA_ASISTENCIA) {
            tramos.add(new TramoAsistencia(desde, tramo[0], tramo[1], tramo[1] * NOTA_INICIAL / ASIGNACION_MAXIMA));
            desde = tramo[0];
        }
        tramos.add(new TramoAsistencia(desde, null, 0, 0));
        return tramos;
    }

    /** Porcentaje que se asigna según la tabla del artículo 37 (de 0 a {@link #ASIGNACION_MAXIMA}). */
    public static int asignacionAsistencia(double porcentajeAusencias) {
        for (int[] tramo : TABLA_ASISTENCIA) {
            if (porcentajeAusencias < tramo[0]) {
                return tramo[1];
            }
        }
        return 0;
    }

    /**
     * Nota del rubro Asistencia de 0 a 100: la parte de la asignación máxima que conserva el estudiante
     * (5 de 5 = 100, 4 de 5 = 80, …). Así, con un peso de 5% en el componente, aporta exactamente el porcentaje
     * de la tabla.
     */
    public int notaAsistencia(int[] conteo, int lecciones) {
        if (lecciones <= 0) {
            return NOTA_INICIAL;
        }
        return asignacionAsistencia(porcentajeAusencias(conteo, lecciones)) * NOTA_INICIAL / ASIGNACION_MAXIMA;
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

    /** Cada período parte de 100. El promedio anual se calcula aparte, con esas notas. */
    public int nota(int puntosBoletas, int llamadas, int[] conteo) {
        int descuento = Math.max(0, puntosBoletas) + puntos(TipoRebaja.LLAMADA, llamadas) + puntosAsistencia(conteo);
        return Math.max(0, NOTA_INICIAL - descuento);
    }
}
