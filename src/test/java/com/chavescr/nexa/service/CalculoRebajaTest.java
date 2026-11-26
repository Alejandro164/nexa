package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
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
    void laAsistenciaSigueLaTablaDelArticulo37() {
        CalculoRebaja calculo = new CalculoRebaja(Map.of(), false, 2, true);

        assertEquals(100, calculo.notaAsistencia(conteo(0, 0, 0, 0), 20));
        assertEquals(100, calculo.notaAsistencia(conteo(0, 1, 0, 0), 20));
        assertEquals(80, calculo.notaAsistencia(conteo(0, 2, 0, 0), 20));
        assertEquals(60, calculo.notaAsistencia(conteo(0, 4, 0, 0), 20));
        assertEquals(40, calculo.notaAsistencia(conteo(0, 6, 0, 0), 20));
        assertEquals(20, calculo.notaAsistencia(conteo(0, 9, 0, 0), 20));
        assertEquals(0, calculo.notaAsistencia(conteo(0, 10, 0, 0), 20));
    }

    @Test
    void lasJustificadasNoCuentanYSinLeccionesQuedaEnCien() {
        CalculoRebaja calculo = new CalculoRebaja(Map.of(), false, 2, true);

        assertEquals(100, calculo.notaAsistencia(conteo(8, 0, 8, 0), 20));
        assertEquals(100, calculo.notaAsistencia(conteo(0, 3, 0, 0), 0));
    }

    @Test
    void cadaTardiaInjustificadaValeMediaAusenciaOUnaSegunLaConfiguracion() {
        CalculoRebaja media = new CalculoRebaja(Map.of(), false, 2, true);
        CalculoRebaja completa = new CalculoRebaja(Map.of(), false, 1, true);

        assertEquals(2.0, media.ausenciasEquivalentes(conteo(0, 0, 0, 4)));
        assertEquals(80, media.notaAsistencia(conteo(0, 0, 0, 4), 20));
        assertEquals(4.0, completa.ausenciasEquivalentes(conteo(0, 0, 0, 4)));
        assertEquals(60, completa.notaAsistencia(conteo(0, 0, 0, 4), 20));
    }

    @Test
    void cadaGrupoCompletoDeJustificadasSumaUnaAusenciaInjustificada() {
        CalculoRebaja cadaTres = new CalculoRebaja(Map.of(), false, 2, 3, true);

        // 8 justificadas: dos grupos de 3 suman 2 ausencias; las 2 que sobran no cuentan.
        assertEquals(2, cadaTres.ausenciasPorJustificadas(conteo(8, 0, 0, 0)));
        assertEquals(2.0, cadaTres.ausenciasEquivalentes(conteo(8, 0, 0, 0)));
        assertEquals(80, cadaTres.notaAsistencia(conteo(8, 0, 0, 0), 20));
        // Se suman a las injustificadas y a las tardías; las tardías justificadas nunca cuentan.
        assertEquals(4.0, cadaTres.ausenciasEquivalentes(conteo(6, 1, 9, 2)));
        assertEquals(3, cadaTres.getJustificadasPorAusencia());
    }

    @Test
    void conUnaJustificadaPorAusenciaCadaJustificadaCuentaComoInjustificada() {
        CalculoRebaja una = new CalculoRebaja(Map.of(), false, 2, 1, true);

        assertEquals(4.0, una.ausenciasEquivalentes(conteo(4, 0, 0, 0)));
        assertEquals(60, una.notaAsistencia(conteo(4, 0, 0, 0), 20));
    }

    @Test
    void laLeyendaDeNotasExplicaCuantasJustificadasFormanUnaAusencia() {
        CalculoRebaja cadaTres = new CalculoRebaja(Map.of(), false, 2, 3, true);

        String leyenda = NotasConsultaService.leyendaAsistencia(cadaTres, conteo(7, 1, 0, 0), 20, 80, 5, "Ciencias");

        assertEquals("Ausencias injustificadas: 3 de 20 lecciones (cada tardía injustificada cuenta como media "
                + "ausencia; cada 3 ausencias justificadas forman una injustificada, aquí suman 2), 15.0% del total. "
                + "Según el artículo 37 se asigna 4 de 5, así que la asistencia queda en 80. Vale 5% de la nota de "
                + "Ciencias.", leyenda);
    }

    @Test
    void sinConfigurarLasJustificadasSiguenSinContar() {
        CalculoRebaja calculo = new CalculoRebaja(Map.of(), false, 2, true);

        assertEquals(0, calculo.getJustificadasPorAusencia());
        assertEquals(0, calculo.ausenciasPorJustificadas(conteo(30, 0, 0, 0)));
        assertEquals(100, calculo.notaAsistencia(conteo(30, 0, 0, 0), 20));
    }

    @Test
    void conValoresPropiosLosGruposDeJustificadasEntranALaReglaDeInjustificadas() {
        Map<TipoRebaja, int[]> reglas = Map.of(
                TipoRebaja.AUSENCIA_INJUSTIFICADA, new int[] { 2, 5 },
                TipoRebaja.AUSENCIA_JUSTIFICADA, new int[] { 1, 1 });
        CalculoRebaja sinAjuste = new CalculoRebaja(reglas, true, 2, 0, false);
        CalculoRebaja cadaTres = new CalculoRebaja(reglas, true, 2, 3, false);

        // Sin ajuste: cada justificada baja 1 por su propia regla; la injustificada sola no completa grupo.
        assertEquals(7, sinAjuste.rebajaAsistencia(conteo(7, 1, 0, 0)));
        // Con 3: 7 justificadas forman 2 injustificadas; 1 + 2 = 3 injustificadas → un grupo de 2 → 5 puntos.
        // La regla de justificadas deja de aplicar.
        assertEquals(5, cadaTres.rebajaAsistencia(conteo(7, 1, 0, 0)));
        assertEquals(95, cadaTres.notaAsistencia(conteo(7, 1, 0, 0), 20));
        assertEquals(5, cadaTres.puntosAsistencia(conteo(7, 1, 0, 0)));
    }

    @Test
    void losPuntosPorRegistroCarganEnLaJustificadaQueCompletaElGrupo() {
        Map<TipoRebaja, int[]> reglas = Map.of(
                TipoRebaja.AUSENCIA_INJUSTIFICADA, new int[] { 2, 5 },
                TipoRebaja.AUSENCIA_JUSTIFICADA, new int[] { 1, 1 },
                TipoRebaja.TARDIA_INJUSTIFICADA, new int[] { 2, 1 });
        CalculoRebaja cadaTres = new CalculoRebaja(reglas, true, 2, 3, false);
        List<EstadoAsistencia> enOrden = List.of(
                EstadoAsistencia.JUSTIFICADA, EstadoAsistencia.JUSTIFICADA, EstadoAsistencia.JUSTIFICADA,
                EstadoAsistencia.AUSENTE, EstadoAsistencia.TARDIA, EstadoAsistencia.TARDIA,
                EstadoAsistencia.JUSTIFICADA, EstadoAsistencia.JUSTIFICADA, EstadoAsistencia.JUSTIFICADA,
                EstadoAsistencia.AUSENTE, EstadoAsistencia.PRESENTE);

        int[] marcas = cadaTres.puntosPorRegistroAsistencia(enOrden);

        // La 3.ª justificada es la 1.ª injustificada; la ausencia siguiente completa el grupo de 2 y baja 5.
        // La 6.ª justificada es la 3.ª injustificada y la última ausencia, la 4.ª: otro grupo, otros 5.
        assertArrayEquals(new int[] { 0, 0, 0, 5, 0, 1, 0, 0, 0, 5, 0 }, marcas);
        assertEquals(cadaTres.puntosAsistencia(conteo(6, 2, 0, 2)), Arrays.stream(marcas).sum());
    }

    @Test
    void sinAjusteLosPuntosPorRegistroSonLosDeSiempre() {
        Map<TipoRebaja, int[]> reglas = Map.of(
                TipoRebaja.AUSENCIA_INJUSTIFICADA, new int[] { 2, 5 },
                TipoRebaja.AUSENCIA_JUSTIFICADA, new int[] { 1, 1 });
        CalculoRebaja calculo = new CalculoRebaja(reglas, true, 2, 0, false);
        CalculoRebaja enComponente = new CalculoRebaja(reglas, false, 2, 3, false);
        List<EstadoAsistencia> enOrden = List.of(EstadoAsistencia.JUSTIFICADA, EstadoAsistencia.AUSENTE,
                EstadoAsistencia.JUSTIFICADA, EstadoAsistencia.AUSENTE);

        assertArrayEquals(new int[] { 1, 0, 1, 5 }, calculo.puntosPorRegistroAsistencia(enOrden));
        // Si la asistencia no rebaja la conducta, los detalles de conducta no muestran puntos.
        assertArrayEquals(new int[] { 0, 0, 0, 0 }, enComponente.puntosPorRegistroAsistencia(enOrden));
    }

    @Test
    void conValoresPropiosLaAsistenciaParteDeCienYRestaLosPuntos() {
        CalculoRebaja calculo = calculo(false, TipoRebaja.AUSENCIA_INJUSTIFICADA, 3, 5);

        assertEquals(10, calculo.rebajaAsistencia(conteo(0, 7, 0, 0)));
        assertEquals(90, calculo.notaAsistencia(conteo(0, 7, 0, 0), 20));
    }

    @Test
    void conElReglamentoLaAsistenciaNuncaRebajaLaConducta() {
        CalculoRebaja calculo = new CalculoRebaja(Map.of(TipoRebaja.AUSENCIA_INJUSTIFICADA, new int[] { 1, 5 }),
                true, 2, true);

        assertEquals(false, calculo.asistenciaRebajaConducta());
        assertEquals(0, calculo.puntosAsistencia(conteo(0, 3, 0, 0)));
    }

    @Test
    void laTablaParaMostrarTieneSeisTramosDeCincoACero() {
        List<CalculoRebaja.TramoAsistencia> tramos = CalculoRebaja.tramosAsistencia();

        assertEquals(6, tramos.size());
        assertEquals(new CalculoRebaja.TramoAsistencia(0, 10, 5, 100), tramos.get(0));
        assertEquals(new CalculoRebaja.TramoAsistencia(50, null, 0, 0), tramos.get(5));
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
