package com.chavescr.nexa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BloqueoLeccionTest {

    @Test
    void aplicaSoloCuandoCoincidenGradoDiaYLeccion() {
        BloqueoLeccion regla = cerrada(List.of(5, 6), List.of("LUNES", "MARTES"), List.of(10));

        assertTrue(regla.aplica(10, "LUNES", 5));
        assertFalse(regla.aplica(7, "LUNES", 5));
        assertFalse(regla.aplica(10, "MIERCOLES", 5));
        assertFalse(regla.aplica(10, "LUNES", 4));
    }

    @Test
    void noSolapaSiElGradoElDiaOLaLeccionSonDistintos() {
        BloqueoLeccion lunesDecimo = cerrada(List.of(5), List.of("LUNES"), List.of(10));

        assertFalse(lunesDecimo.solapaCon(cerrada(List.of(5), List.of("LUNES"), List.of(7))));
        assertFalse(lunesDecimo.solapaCon(cerrada(List.of(5), List.of("MARTES"), List.of(10))));
        assertFalse(lunesDecimo.solapaCon(cerrada(List.of(6), List.of("LUNES"), List.of(10))));
    }

    @Test
    void solapaCuandoCompartenUnaCeldaAunqueElRestoNo() {
        BloqueoLeccion amplia = cerrada(List.of(5, 6), List.of("LUNES", "MARTES"), List.of(10, 11));
        BloqueoLeccion puntual = cerrada(List.of(5), List.of("LUNES"), List.of(10));

        assertTrue(amplia.solapaCon(puntual));
        assertTrue(puntual.solapaCon(amplia));
    }

    @Test
    void vigenteDevuelveLaPrimeraReglaQueCubreLaCelda() {
        BloqueoLeccion primera = cerrada(List.of(5), List.of("LUNES"), List.of(10));
        BloqueoLeccion segunda = cerrada(List.of(5), List.of("LUNES"), List.of(10));
        segunda.setMotivo("no debería ganar");

        BloqueoLeccion hallada = BloqueoLeccion.vigente(
                List.of(primera, segunda), 10, "LUNES", 5);

        assertSame(primera, hallada);
        assertNull(BloqueoLeccion.vigente(List.of(primera), 7, "LUNES", 5));
    }

    @Test
    void exigirSinSolapesRechazaUnaLeccionYaBloqueada() {
        BloqueoLeccion existente = cerrada(List.of(5), List.of("LUNES"), List.of(10));
        BloqueoLeccion duplicada = tipo(List.of(5, 6), List.of("LUNES"), List.of(10));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> BloqueoLeccion.exigirSinSolapes(List.of(existente, duplicada)));

        assertEquals(
                "La lección 5 del lunes en grado 10 ya está bloqueada. Quítala de las reglas activas si quieres cambiarla.",
                error.getMessage());
    }

    @Test
    void exigirSinSolapesPermiteLaMismaLeccionEnOtroGrado() {
        BloqueoLeccion.exigirSinSolapes(List.of(
                cerrada(List.of(5), List.of("LUNES"), List.of(10)),
                tipo(List.of(5), List.of("LUNES"), List.of(7))));
    }

    private BloqueoLeccion cerrada(List<Integer> lecciones, List<String> dias, List<Integer> grados) {
        BloqueoLeccion regla = new BloqueoLeccion();
        regla.setListaLecciones(lecciones);
        regla.setListaDias(dias);
        regla.setListaGrados(grados);
        regla.setModo(BloqueoLeccion.MODO_CERRADA);
        return regla;
    }

    private BloqueoLeccion tipo(List<Integer> lecciones, List<String> dias, List<Integer> grados) {
        BloqueoLeccion regla = cerrada(lecciones, dias, grados);
        regla.setModo(BloqueoLeccion.MODO_TIPO);
        return regla;
    }
}
