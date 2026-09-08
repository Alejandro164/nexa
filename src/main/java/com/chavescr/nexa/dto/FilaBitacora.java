package com.chavescr.nexa.dto;

import java.time.LocalDateTime;

import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.BitacoraEvento;
import com.chavescr.nexa.entity.HistorialCambio;
import com.chavescr.nexa.entity.ModuloSistema;

public class FilaBitacora {

    private final LocalDateTime fecha;
    private final ModuloSistema modulo;
    private final String area;
    private final AccionHistorial accion;
    private final String itemTitulo;
    private final String usuarioNombre;
    private final String detalle;

    public FilaBitacora(LocalDateTime fecha, ModuloSistema modulo, String area, AccionHistorial accion,
            String itemTitulo, String usuarioNombre, String detalle) {
        this.fecha = fecha;
        this.modulo = modulo;
        this.area = area;
        this.accion = accion;
        this.itemTitulo = itemTitulo;
        this.usuarioNombre = usuarioNombre;
        this.detalle = detalle;
    }

    public static FilaBitacora desde(BitacoraEvento evento) {
        return new FilaBitacora(evento.getFecha(), evento.getModulo(), evento.getArea(), evento.getAccion(),
                evento.getItemTitulo(), evento.getUsuarioNombre(), evento.getDetalle());
    }

    public static FilaBitacora desde(HistorialCambio evento) {
        return new FilaBitacora(evento.getFecha(), ModuloSistema.GESTION_ACADEMICA,
                ModuloSistema.areaDe(evento.getModulo()), evento.getAccion(), evento.getItemTitulo(),
                evento.getUsuarioNombre(), evento.getDetalle());
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public ModuloSistema getModulo() {
        return modulo;
    }

    public String getArea() {
        return area;
    }

    public AccionHistorial getAccion() {
        return accion;
    }

    public String getItemTitulo() {
        return itemTitulo;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public String getDetalle() {
        return detalle;
    }
}
