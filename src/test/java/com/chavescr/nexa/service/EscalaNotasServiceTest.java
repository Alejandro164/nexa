package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.chavescr.nexa.dto.VistaEscala;
import com.chavescr.nexa.entity.EscalaNotas;

class EscalaNotasServiceTest {

    @Test
    void laEscalaPredeterminadaSeGuardaIgual() {
        EscalaNotas escala = EscalaNotas.predeterminada();
        String canonico = EscalaNotasService.canonizar(
                escala.getNotaMinima(),
                escala.getNotaMaxima(),
                escala.getDecimales(),
                escala.getNotaAprobacion(),
                escala.getTramos());

        assertEquals(EscalaNotas.TRAMOS_PREDETERMINADOS, canonico);
    }

    @Test
    void apruebaDesdeOtroTramo() {
        String canonico = EscalaNotasService.canonizar(0, 100, 1, 80, EscalaNotas.TRAMOS_PREDETERMINADOS);

        assertEquals(EscalaNotas.TRAMOS_PREDETERMINADOS, canonico);
    }

    @Test
    void conservaElNombreConComillas() {
        String json = "[{\"nombre\":\"Muy \\\"bien\\\"\",\"desde\":90},{\"nombre\":\"Aplazado\",\"desde\":0}]";

        String canonico = EscalaNotasService.canonizar(0, 100, 0, 90, json);

        assertEquals(json, canonico);
    }

    @Test
    void vistaDaUnTonoPorTramoSegunElCorte() {
        VistaEscala vista = EscalaNotasService.vista(EscalaNotas.predeterminada());

        assertEquals(List.of("excelente", "bueno", "regular", "aplazado"), tonos(vista));
        assertEquals("excelente", vista.tono(100));
        assertEquals("bueno", vista.tono(89.9));
        assertEquals("regular", vista.tono(70));
        assertEquals("aplazado", vista.tono(69.9));
    }

    @Test
    void tonoNoRedondeaLaNota() {
        VistaEscala vista = EscalaNotasService.vista(EscalaNotas.predeterminada());

        assertEquals("aplazado", vista.tono(69.99));
        assertEquals("regular", vista.tono(70.00));
    }

    @Test
    void tramoDevuelveElNombreAsignado() {
        VistaEscala vista = EscalaNotasService.vista(EscalaNotas.predeterminada());

        assertEquals("Excelente", vista.tramo(95).nombre());
        assertEquals("excelente", vista.tramo(95).tono());
        assertEquals("Bueno", vista.tramo(80).nombre());
        assertEquals("Aplazado", vista.tramo(69).nombre());
    }

    @Test
    void elCorteCambiaElTonoYConservaElNombre() {
        EscalaNotas escala = EscalaNotas.predeterminada();
        escala.setNotaAprobacion(80);
        VistaEscala vista = EscalaNotasService.vista(escala);

        assertEquals("Bueno", vista.tramo(80).nombre());
        assertEquals("regular", vista.tramo(80).tono());
    }

    @Test
    void vistaBajaDeTonoLoQueQuedaBajoElCorte() {
        EscalaNotas escala = EscalaNotas.predeterminada();
        escala.setNotaAprobacion(80);

        assertEquals(List.of("excelente", "regular", "aplazado", "aplazado"),
                tonos(EscalaNotasService.vista(escala)));
    }

    @Test
    void vistaInvalidaVuelveALaEscalaEstandar() {
        EscalaNotas escala = EscalaNotas.predeterminada();
        escala.setTramos("no-es-json");

        assertEquals(List.of("excelente", "bueno", "regular", "aplazado"),
                tonos(EscalaNotasService.vista(escala)));
    }

    private static List<String> tonos(VistaEscala vista) {
        return vista.tramos().stream().map(VistaEscala.Tramo::tono).toList();
    }

    @Test
    void guardaSoloNombreEInicio() {
        String json = "[{\"nombre\":\"Excelente\",\"desde\":90,\"hasta\":100,\"incluyeHasta\":true,\"aprueba\":true},"
                + "{\"nombre\":\"Aplazado\",\"desde\":0,\"hasta\":90,\"incluyeHasta\":false,\"aprueba\":false}]";

        String canonico = EscalaNotasService.canonizar(0, 100, 1, 90, json);

        assertEquals("[{\"nombre\":\"Excelente\",\"desde\":90},{\"nombre\":\"Aplazado\",\"desde\":0}]", canonico);
    }

    @Test
    void rechazaAprobarDesdeElTramoMasBajo() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> EscalaNotasService.canonizar(0, 100, 1, 0, EscalaNotas.TRAMOS_PREDETERMINADOS));

        assertEquals("Elige el tramo desde el que se aprueba", error.getMessage());
    }

    @Test
    void rechazaNombresRepetidos() {
        String json = "[{\"nombre\":\"Bueno\",\"desde\":90},{\"nombre\":\"bueno\",\"desde\":0}]";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> EscalaNotasService.canonizar(0, 100, 1, 90, json));

        assertEquals("Los nombres de los tramos no se pueden repetir", error.getMessage());
    }
}
