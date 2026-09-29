package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.ClassroomClase;
import com.chavescr.nexa.entity.Usuario;

/** Clase abierta por el usuario actual, con su papel en ella ya resuelto (y validado). */
public class ContextoClase {

    private final ClassroomClase clase;
    private final RolClase rol;
    private final Usuario usuario;
    private final List<Usuario> docentes;

    public ContextoClase(ClassroomClase clase, RolClase rol, Usuario usuario, List<Usuario> docentes) {
        this.clase = clase;
        this.rol = rol;
        this.usuario = usuario;
        this.docentes = docentes;
    }

    public Long getDireccionId() {
        return clase.getDireccion().getId();
    }

    public ClassroomClase getClase() {
        return clase;
    }

    public RolClase getRol() {
        return rol;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public List<Usuario> getDocentes() {
        return docentes;
    }

    public String getNombresDocentes() {
        return docentes.stream().map(Usuario::getNombre).reduce((a, b) -> a + ", " + b).orElse("Sin docente");
    }
}
