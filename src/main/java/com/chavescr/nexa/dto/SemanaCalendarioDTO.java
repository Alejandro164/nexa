package com.chavescr.nexa.dto;

import java.util.List;

/** Una semana (7 días) del calendario, junto con las bandas de eventos de varios días que la cruzan. */
public class SemanaCalendarioDTO {

    /** Filas de bandas que se muestran sin necesidad de expandir. */
    public static final int FILAS_VISIBLES_POR_DEFECTO = 3;

    private final List<DiaCalendarioDTO> dias;
    private final List<BandaEventoDTO> bandas;

    public SemanaCalendarioDTO(List<DiaCalendarioDTO> dias, List<BandaEventoDTO> bandas) {
        this.dias = dias;
        this.bandas = bandas;
    }

    public List<DiaCalendarioDTO> getDias() { return dias; }
    public List<BandaEventoDTO> getBandas() { return bandas; }

    /** Cuántas filas de bandas hay en total (incluyendo las que solo se ven al expandir). */
    public int getFilasBandas() {
        return bandas.stream().mapToInt(BandaEventoDTO::getFila).max().orElse(-1) + 1;
    }

    /** Cuántos eventos de varios días quedan fuera de las filas visibles por defecto. */
    public int getBandasOcultas() {
        return (int) bandas.stream().filter(b -> b.getFila() >= FILAS_VISIBLES_POR_DEFECTO).count();
    }

    public boolean isTieneBandasOcultas() {
        return getBandasOcultas() > 0;
    }

    /** Alto (px) de la franja de bandas colapsada, mostrando solo las filas visibles por defecto. */
    public int getAlturaColapsadaPx() {
        return alturaParaFilas(Math.min(getFilasBandas(), FILAS_VISIBLES_POR_DEFECTO));
    }

    /** Alto (px) de la franja de bandas expandida, mostrando todas las filas. */
    public int getAlturaExpandidaPx() {
        return alturaParaFilas(getFilasBandas());
    }

    private static int alturaParaFilas(int filas) {
        return filas <= 0 ? 0 : filas * 24 + 6;
    }
}
