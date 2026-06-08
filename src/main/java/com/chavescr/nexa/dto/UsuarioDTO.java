package com.chavescr.nexa.dto;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import com.chavescr.nexa.entity.Usuario;

public class UsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String email;
    private String usuario;
    private String cedula;
    private boolean activo;
    private Set<String> roles;

    public UsuarioDTO() {
    }

    public UsuarioDTO(Usuario u) {
        this.id = u.getId();
        this.nombre = u.getNombre();
        this.email = u.getEmail();
        this.usuario = u.getUsuario();
        this.cedula = u.getCedula();
        this.activo = u.getActivo();
        this.roles = u.getRoles().stream()
                .map(r -> r.getNombre())
                .collect(Collectors.toSet());
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
