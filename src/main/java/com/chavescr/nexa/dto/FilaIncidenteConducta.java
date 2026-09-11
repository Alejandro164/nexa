package com.chavescr.nexa.dto;

import java.time.LocalDate;

import com.chavescr.nexa.entity.Usuario;

public class FilaIncidenteConducta {

    private final Long id;
    private final String numero;
    private final Usuario estudiante;
    private final String seccion;
    private final LocalDate fecha;
    private final String motivo;
    private final String descripcion;
    private final int puntos;
    private final String docenteNombre;
    private final String estadoCss;
    private final String estadoLabel;
    private final boolean puedeResolver;
    private final String iniciales;
    private final String colorAvatar;

    public FilaIncidenteConducta(Long id, String numero, Usuario estudiante, String seccion, LocalDate fecha,
            String motivo, String descripcion, int puntos, String docenteNombre, String estadoCss, String estadoLabel,
            boolean puedeResolver, String iniciales, String colorAvatar) {
        this.id = id;
        this.numero = numero;
        this.estudiante = estudiante;
        this.seccion = seccion;
        this.fecha = fecha;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.puntos = puntos;
        this.docenteNombre = docenteNombre;
        this.estadoCss = estadoCss;
        this.estadoLabel = estadoLabel;
        this.puedeResolver = puedeResolver;
        this.iniciales = iniciales;
        this.colorAvatar = colorAvatar;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public String getSeccion() {
        return seccion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPuntos() {
        return puntos;
    }

    public String getDocenteNombre() {
        return docenteNombre;
    }

    public String getEstadoCss() {
        return estadoCss;
    }

    public String getEstadoLabel() {
        return estadoLabel;
    }

    public boolean isPuedeResolver() {
        return puedeResolver;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getColorAvatar() {
        return colorAvatar;
    }
}
