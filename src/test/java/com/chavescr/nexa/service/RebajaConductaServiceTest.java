package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chavescr.nexa.entity.DestinoRebajaAsistencia;
import com.chavescr.nexa.entity.QuienRegistraPuntosBoleta;
import com.chavescr.nexa.entity.RebajaConducta;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.RebajaConductaRepository;

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

    @Test
    void pideQuienRegistraLosPuntosDeLasBoletas() {
        RebajaConductaService service = new RebajaConductaService(null, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(1L, RebajaConducta.REGLAS_PREDETERMINADAS, null, 2));

        assertEquals("Indica quién registra los puntos de las boletas", error.getMessage());
    }

    @Test
    void pideCuantoValeCadaTardiaInjustificada() {
        RebajaConductaService service = new RebajaConductaService(null, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(1L, RebajaConducta.REGLAS_PREDETERMINADAS,
                        QuienRegistraPuntosBoleta.PROFESOR_GUIA, 3));

        assertEquals("Indica cuánto vale cada tardía injustificada", error.getMessage());
    }

    @Test
    void lasAusenciasYaNoRebajanLaConductaAunqueElDestinoViejoDigaConducta() {
        CalculoRebaja calculo = calculoDe(DestinoRebajaAsistencia.CONDUCTA, grupos());

        assertEquals(0, calculo.puntosAsistencia(new int[] { 2, 4, 0, 3 }));
        assertEquals(90, calculo.nota(10, new int[] { 2, 4, 0, 3 }));
        assertTrue(new RebajaConductaService(null, null).asistenciaRebajaComponente(2L));
    }

    @Test
    void elCalculoUsaLaLlamadaDeAtencionGuardada() {
        CalculoRebaja calculo = calculoDe(DestinoRebajaAsistencia.COMPONENTE, grupos());

        assertEquals(10, calculo.puntos(TipoRebaja.LLAMADA, 2));
    }

    @Test
    void unTextoIlegibleVuelveALasReglasPredeterminadas() {
        CalculoRebaja calculo = calculoDe(DestinoRebajaAsistencia.COMPONENTE, "no-es-json");

        assertEquals(5, calculo.puntos(TipoRebaja.LLAMADA, 1));
    }

    @Test
    void elCalculoUsaLaEquivalenciaDeTardiasGuardada() {
        RebajaConductaRepository repository = mock(RebajaConductaRepository.class);
        RebajaConductaService service = new RebajaConductaService(repository, mock(DireccionRepository.class));
        RebajaConducta rebaja = new RebajaConducta();
        rebaja.setTardiasPorAusencia(1);
        when(repository.findByDireccionId(2L)).thenReturn(Optional.of(rebaja));

        assertEquals(1, service.calculo(2L).getTardiasPorAusencia());
    }

    private static CalculoRebaja calculoDe(DestinoRebajaAsistencia destino, String reglas) {
        RebajaConductaRepository repository = mock(RebajaConductaRepository.class);
        RebajaConductaService service = new RebajaConductaService(repository, mock(DireccionRepository.class));
        RebajaConducta rebaja = new RebajaConducta();
        rebaja.setDestinoRebajaAsistencia(destino);
        rebaja.setReglas(reglas);
        when(repository.findByDireccionId(2L)).thenReturn(Optional.of(rebaja));
        return service.calculo(2L);
    }

    private static String grupos() {
        return "["
                + "{\"id\":\"llamada\",\"cada\":1,\"puntos\":5},"
                + "{\"id\":\"tardia-injustificada\",\"cada\":2,\"puntos\":2},"
                + "{\"id\":\"tardia-justificada\",\"cada\":1,\"puntos\":0},"
                + "{\"id\":\"ausencia-injustificada\",\"cada\":3,\"puntos\":5},"
                + "{\"id\":\"ausencia-justificada\",\"cada\":1,\"puntos\":1}"
                + "]";
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
