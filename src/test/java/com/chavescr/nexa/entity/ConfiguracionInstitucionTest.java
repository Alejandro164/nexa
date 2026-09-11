package com.chavescr.nexa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConfiguracionInstitucionTest {

    @Test
    void relojPredeterminadoSigueElPatronMepDeRecreosDistintos() {
        ConfiguracionInstitucion config = ConfiguracionInstitucion.predeterminada(null);

        assertEquals(LocalTime.of(7, 0), config.horaInicioLeccion(1));
        assertEquals(LocalTime.of(7, 40), config.horaFinLeccion(1));
        assertTrue(config.hayRecreoDespues(2));
        assertEquals(15, config.minutosRecreoDespues(2));
        assertEquals(LocalTime.of(8, 20), config.recreos().get(2).inicio());
        assertEquals(15, config.recreos().get(2).getMinutos());
        assertEquals(LocalTime.of(8, 35), config.horaInicioLeccion(3));

        assertEquals(20, config.minutosRecreoDespues(4));
        assertEquals(LocalTime.of(9, 55), config.recreos().get(4).inicio());
        assertEquals(LocalTime.of(10, 15), config.horaInicioLeccion(5));

        assertTrue(config.hayAlmuerzoDespues(6));
        assertFalse(config.hayRecreoDespues(6));
        assertEquals(LocalTime.of(11, 35), config.almuerzos().get(6).inicio());
        assertEquals(LocalTime.of(12, 15), config.horaInicioLeccion(7));

        assertEquals(10, config.minutosRecreoDespues(8));
        assertEquals("recreos de 15, 20, 10 y 10 min", config.etiquetaRecreos());
    }

    @Test
    void sinMinutosDeAlmuerzoElTercerRecesoOcupaEseHueco() {
        ConfiguracionInstitucion config = ConfiguracionInstitucion.predeterminada(null);
        config.setMinutosAlmuerzo(0);

        assertFalse(config.hayAlmuerzoDespues(6));
        assertTrue(config.hayRecreoDespues(6));
        assertEquals(10, config.minutosRecreoDespues(6));
        assertEquals(LocalTime.of(11, 35), config.recreos().get(6).inicio());
        assertEquals(LocalTime.of(11, 45), config.horaInicioLeccion(7));
    }

    @Test
    void recreoEnCeroNoInsertaPausaEnEseBloque() {
        ConfiguracionInstitucion config = ConfiguracionInstitucion.predeterminada(null);
        config.setListaDuracionesRecreos(List.of(15, 0, 10));

        assertTrue(config.hayRecreoDespues(2));
        assertFalse(config.hayRecreoDespues(4));
        assertEquals(LocalTime.of(8, 35), config.horaInicioLeccion(3));
        assertEquals(LocalTime.of(9, 15), config.horaInicioLeccion(4));
        assertEquals(LocalTime.of(9, 55), config.horaInicioLeccion(5));
    }
}
