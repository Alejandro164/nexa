package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_academico")
public class HistorialCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institucion_id", nullable = false)
    private Long institucionId;

    @Column(name = "nivel_id", nullable = false)
    private Long nivelId;

    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModuloAcademico modulo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_titulo", nullable = false, length = 200)
    private String itemTitulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccionHistorial accion;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInstitucionId() {
        return institucionId;
    }

    public void setInstitucionId(Long institucionId) {
        this.institucionId = institucionId;
    }

    public Long getNivelId() {
        return nivelId;
    }

    public void setNivelId(Long nivelId) {
        this.nivelId = nivelId;
    }

    public Long getMateriaId() {
        return materiaId;
    }

    public void setMateriaId(Long materiaId) {
        this.materiaId = materiaId;
    }

    public ModuloAcademico getModulo() {
        return modulo;
    }

    public void setModulo(ModuloAcademico modulo) {
        this.modulo = modulo;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitulo() {
        return itemTitulo;
    }

    public void setItemTitulo(String itemTitulo) {
        this.itemTitulo = itemTitulo;
    }

    public AccionHistorial getAccion() {
        return accion;
    }

    public void setAccion(AccionHistorial accion) {
        this.accion = accion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
