package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.chavescr.nexa.entity.RebajaConducta;

class RebajaConductaServiceTest {

    @Test
    void laConfiguracionPredeterminadaSeGuardaIgual() {
        assertEquals(RebajaConducta.REGLAS_PREDETERMINADAS,
                RebajaConductaService.canonizar(RebajaConducta.REGLAS_PREDETERMINADAS));
    }

    @Test
    void ordenaLasCincoRebajasAunqueLleguenRevueltas() {
        String json = "["
                + "{\"id\":\"ausencia-justificada\",\"puntos\":0,\"cada\":1},"
                + "{\"id\":\"llamada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":1,\"puntos\":2}"
                + "]";

        assertEquals(RebajaConducta.REGLAS_PREDETERMINADAS, RebajaConductaService.canonizar(json));
    }

    @Test
    void conservaUnaRebajaDistintaPorRegistro() {
        String json = "["
                + "{\"id\":\"llamada\",\"cada\":2,\"puntos\":5},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":1,\"puntos\":2},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":3,\"puntos\":5},"
                + "{\"id\":\"ausencia-justificada\",\"cada\":1,\"puntos\":0}"
                + "]";

        String canonico = RebajaConductaService.canonizar(json);

        assertEquals("["
                + "{\"id\":\"llamada\",\"cada\":2,\"puntos\":5},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":1,\"puntos\":2},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":3,\"puntos\":5},"
                + "{\"id\":\"ausencia-justificada\",\"cada\":1,\"puntos\":0}"
                + "]", canonico);
    }

    @Test
    void rechazaUnGrupoFueraDeRango() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RebajaConductaService.canonizar(conLlamada(0, 5)));

        assertEquals("«Llamada de atención» se cuenta de 1 a 30", error.getMessage());
    }

    @Test
    void rechazaPuntosFueraDeRango() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RebajaConductaService.canonizar(conLlamada(1, 101)));

        assertEquals("Los puntos de «Llamada de atención» van de 0 a 100", error.getMessage());
    }

    @Test
    void rechazaDecimales() {
        assertThrows(IllegalArgumentException.class,
                () -> RebajaConductaService.canonizar(conLlamada("1.5", "5")));
    }

    @Test
    void rechazaUnRegistroQueNoExiste() {
        String json = RebajaConducta.REGLAS_PREDETERMINADAS.replace("]", ",{\"id\":\"boleta\",\"cada\":1,\"puntos\":1}]");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RebajaConductaService.canonizar(json));

        assertEquals("Hay una rebaja que no corresponde", error.getMessage());
    }

    @Test
    void rechazaUnaRebajaRepetida() {
        String json = "["
                + "{\"id\":\"llamada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"llamada\",\"cada\":2,\"puntos\":3},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":1,\"puntos\":2},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"ausencia-justificada\",\"cada\":1,\"puntos\":0}"
                + "]";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RebajaConductaService.canonizar(json));

        assertEquals("La rebaja de «Llamada de atención» está repetida", error.getMessage());
    }

    @Test
    void rechazaSiFaltaUnRegistro() {
        String json = "["
                + "{\"id\":\"llamada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":1,\"puntos\":2},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":1,\"puntos\":5}"
                + "]";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RebajaConductaService.canonizar(json));

        assertEquals("Falta la rebaja de «Ausencia justificada»", error.getMessage());
    }

    private static String conLlamada(int cada, int puntos) {
        return conLlamada(Integer.toString(cada), Integer.toString(puntos));
    }

    private static String conLlamada(String cada, String puntos) {
        return "["
                + "{\"id\":\"llamada\",\"cada\":" + cada + ",\"puntos\":" + puntos + "},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":1,\"puntos\":2},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"ausencia-justificada\",\"cada\":1,\"puntos\":0}"
                + "]";
    }
}
