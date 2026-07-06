package com.chavescr.nexa.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento del calendario oficial del MEP, obtenido de
 * https://calendario.mep.go.cr/{anio}/webservices/obtener_eventos.php
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventoMepDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String titulo;
    private String descripcion;
    private String link;
    private String link2;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String nombreCategoria;
    private List<String> subcategorias;
    private String destacado;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getLink2() { return link2; }
    public void setLink2(String link2) { this.link2 = link2; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
    public List<String> getSubcategorias() { return subcategorias; }
    public void setSubcategorias(List<String> subcategorias) { this.subcategorias = subcategorias; }
    public String getDestacado() { return destacado; }
    public void setDestacado(String destacado) { this.destacado = destacado; }

    public boolean isDestacadoFlag() {
        return "1".equals(destacado);
    }

    public boolean isRangoUnico() {
        return fechaFin == null || fechaFin.equals(fechaInicio);
    }

    public boolean isTieneLink() {
        return link != null && !link.isBlank();
    }
}
