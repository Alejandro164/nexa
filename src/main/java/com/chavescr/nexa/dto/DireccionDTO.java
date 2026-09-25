package com.chavescr.nexa.dto;

import java.io.Serializable;

import com.chavescr.nexa.entity.Direccion;

public class DireccionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String codigo;
    private boolean activa;

    public DireccionDTO() {
    }

    public DireccionDTO(Direccion direccion) {
        this.id = direccion.getId();
        this.nombre = direccion.getPresentacion();
        this.codigo = direccion.getCodigo();
        this.activa = direccion.getActiva() != null ? direccion.getActiva() : false;
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
