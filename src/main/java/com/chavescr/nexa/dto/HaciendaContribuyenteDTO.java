package com.chavescr.nexa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de la API de Consulta de Situación Tributaria del Ministerio de Hacienda
 * (https://api.hacienda.go.cr/fe/ae?identificacion={cedula}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HaciendaContribuyenteDTO {

    private String nombre;
    private String tipoIdentificacion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public void setTipoIdentificacion(String tipoIdentificacion) {
        this.tipoIdentificacion = tipoIdentificacion;
    }
}
