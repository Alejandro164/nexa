package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chavescr.nexa.entity.TipoRebaja;

class CalculoRebajaTest {

    @Test
    void cadaGrupoCompletoBajaLosPuntosConfigurados() {
        CalculoRebaja calculo = calculo(true, TipoRebaja.AUSENCIA_INJUSTIFICADA, 3, 5);

        assertEquals(0, calculo.puntos(TipoRebaja.AUSENCIA_INJUSTIFICADA, 2));
        assertEquals(5, calculo.puntos(TipoRebaja.AUSENCIA_INJUSTIFICADA, 3));
        assertEquals(10, calculo.puntos(TipoRebaja.AUSENCIA_INJUSTIFICADA, 7));
    }

    @Test
    void ceroPuntosNoRebaja() {
        CalculoRebaja calculo = calculo(true, TipoRebaja.AUSENCIA_JUSTIFICADA, 1, 0);

        assertEquals(0, calculo.puntosAsistencia(conteo(4, 0, 0, 0)));
        assertEquals(100, calculo.nota(0, conteo(4, 0, 0, 0)));
    }

    @Test
    void lasAusenciasYTardiasSeSumanALasBoletasYNoBajanDeCero() {
        CalculoRebaja calculo = predeterminada(true);

        assertEquals(78, calculo.nota(10, conteo(0, 2, 0, 1)));
        assertEquals(0, calculo.nota(90, conteo(0, 3, 0, 0)));
    }

    @Test
    void sieteAusenciasCadaTresBajanDiezYLaAsistenciaQuedaEnNoventa() {
        CalculoRebaja calculo = calculo(false, TipoRebaja.AUSENCIA_INJUSTIFICADA, 3, 5);

        assertEquals(10, calculo.rebajaAsistencia(conteo(0, 7, 0, 0)));
        assertEquals(90, calculo.notaAsistencia(conteo(0, 7, 0, 0)));
    }

    @Test
    void elRegistroQueCompletaElGrupoCargaLosPuntos() {
        CalculoRebaja calculo = calculo(true, TipoRebaja.AUSENCIA_INJUSTIFICADA, 3, 5);

        assertEquals(List.of(0, 0, 5, 0, 0, 5), marcas(calculo.puntosPorRegistro(
                TipoRebaja.AUSENCIA_INJUSTIFICADA, 6)));
    }

    @Test
    void unaLlamadaBajaSusPuntosAunqueLaAsistenciaVayaAlComponente() {
        CalculoRebaja calculo = predeterminada(false);

        assertEquals(90, calculo.nota(0, 2, conteo(0, 0, 0, 0)));
        assertEquals(90, calculo.nota(0, 2, conteo(3, 4, 1, 2)));
    }

    @Test
    void siRebajanElComponenteNoRestanDeLaConducta() {
        CalculoRebaja calculo = predeterminada(false);

        assertEquals(0, calculo.puntosAsistencia(conteo(2, 3, 1, 4)));
        assertEquals(90, calculo.nota(10, conteo(2, 3, 1, 4)));
    }

    private static int[] conteo(int justificadas, int injustificadas, int tardiasJustificadas,
            int tardiasInjustificadas) {
        return new int[] { justificadas, injustificadas, tardiasJustificadas, tardiasInjustificadas };
    }

    private static CalculoRebaja predeterminada(boolean enConducta) {
        Map<TipoRebaja, int[]> reglas = new EnumMap<>(TipoRebaja.class);
        for (TipoRebaja tipo : TipoRebaja.values()) {
            reglas.put(tipo, new int[] { tipo.cada(), tipo.puntos() });
        }
        return new CalculoRebaja(reglas, enConducta);
    }

    private static List<Integer> marcas(int[] puntos) {
        return Arrays.stream(puntos).boxed().toList();
    }

    private static CalculoRebaja calculo(boolean enConducta, TipoRebaja tipo, int cada, int puntos) {
        Map<TipoRebaja, int[]> reglas = new EnumMap<>(TipoRebaja.class);
        reglas.put(tipo, new int[] { cada, puntos });
        return new CalculoRebaja(reglas, enConducta);
    }
}
