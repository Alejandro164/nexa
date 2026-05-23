package com.empresa.proyecto.dto;

import com.empresa.proyecto.entity.Empresa;

import java.io.Serializable;

public class EmpresaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String cedula;
    private String direccion;
    private boolean activa;

    public EmpresaDTO() {}

    public EmpresaDTO(Empresa empresa) {
        this.id = empresa.getId();
        this.nombre = empresa.getNombre();
        this.cedula = empresa.getCedula();
        this.direccion = empresa.getDireccion();
        this.activa = empresa.getActiva();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
