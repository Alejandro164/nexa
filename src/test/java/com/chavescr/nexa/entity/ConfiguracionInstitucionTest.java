package com.chavescr.nexa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ConfiguracionInstitucionTest {

    @Test
    void relojPredeterminadoInsertaRecreoYAlmuerzoSinSolaparse() {
        ConfiguracionInstitucion config = ConfiguracionInstitucion.predeterminada(null);

        assertEquals(LocalTime.of(7, 0), config.horaInicioLeccion(1));
        assertEquals(LocalTime.of(7, 40), config.horaFinLeccion(1));
        assertTrue(config.hayRecreoDespues(2));
        assertEquals(LocalTime.of(8, 20), config.recreos().get(2).inicio());
        assertEquals(LocalTime.of(8, 30), config.horaInicioLeccion(3));

        assertTrue(config.hayAlmuerzoDespues(6));
        assertFalse(config.hayRecreoDespues(6));
        assertEquals(LocalTime.of(11, 20), config.almuerzos().get(6).inicio());
        assertEquals(LocalTime.of(12, 0), config.horaInicioLeccion(7));
    }

    @Test
    void sinMinutosDeAlmuerzoNoHayPausaLarga() {
        ConfiguracionInstitucion config = ConfiguracionInstitucion.predeterminada(null);
        config.setMinutosAlmuerzo(0);

        assertFalse(config.hayAlmuerzoDespues(6));
        assertTrue(config.hayRecreoDespues(6));
        assertEquals(LocalTime.of(11, 20), config.recreos().get(6).inicio());
        assertEquals(LocalTime.of(11, 30), config.horaInicioLeccion(7));
    }
}
