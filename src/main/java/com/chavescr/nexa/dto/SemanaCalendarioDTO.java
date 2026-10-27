package com.chavescr.nexa.dto;

import java.util.List;

/** Una semana (7 días) del calendario. */
public class SemanaCalendarioDTO {

    private final List<DiaCalendarioDTO> dias;

    public SemanaCalendarioDTO(List<DiaCalendarioDTO> dias) {
        this.dias = dias;
    }

    public List<DiaCalendarioDTO> getDias() { return dias; }
}
