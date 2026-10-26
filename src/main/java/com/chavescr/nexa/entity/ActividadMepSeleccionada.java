package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Actividad del calendario del MEP que una institución eligió incluir en su calendario. */
@Entity
@Table(name = "actividades_mep_seleccionadas",
        uniqueConstraints = @UniqueConstraint(columnNames = { "direccion_id", "anio", "mep_evento_id" }))
public class ActividadMepSeleccionada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mep_evento_id", nullable = false, length = 100)
    private String mepEventoId;

    @Column(nullable = false)
    private int anio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seleccionado_por_id", nullable = false)
    private Usuario seleccionadoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaSeleccion;

    @PrePersist
    protected void onCreate() {
        this.fechaSeleccion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMepEventoId() { return mepEventoId; }
    public void setMepEventoId(String mepEventoId) { this.mepEventoId = mepEventoId; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public Direccion getDireccion() { return direccion; }
    public void setDireccion(Direccion direccion) { this.direccion = direccion; }
    public Usuario getSeleccionadoPor() { return seleccionadoPor; }
    public void setSeleccionadoPor(Usuario seleccionadoPor) { this.seleccionadoPor = seleccionadoPor; }
    public LocalDateTime getFechaSeleccion() { return fechaSeleccion; }
    public void setFechaSeleccion(LocalDateTime fechaSeleccion) { this.fechaSeleccion = fechaSeleccion; }
}
