package com.chavescr.nexa.dto;

import java.io.Serializable;

import com.chavescr.nexa.entity.Institucion;

public class InstitucionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String codigo;
    private boolean activa;

    public InstitucionDTO() {
    }

    public InstitucionDTO(Institucion institucion) {
        this.id = institucion.getId();
        this.nombre = institucion.getNombre();
        this.codigo = institucion.getCodigo();
        this.activa = institucion.getActiva() != null ? institucion.getActiva() : false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
