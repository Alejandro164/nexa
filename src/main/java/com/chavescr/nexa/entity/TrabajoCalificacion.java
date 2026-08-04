package com.chavescr.nexa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "trabajos_calificaciones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trabajo_calif_estudiante", columnNames = { "trabajo_definicion_id", "estudiante_id" })
})
public class TrabajoCalificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajo_definicion_id", nullable = false)
    private TrabajoDefinicion trabajoDefinicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Column(nullable = false)
    private Integer calificacion;

    @Column(name = "puntos_obtenidos")
    private Integer puntosObtenidos;

    @Column(length = 500)
    private String observacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrabajoDefinicion getTrabajoDefinicion() {
        return trabajoDefinicion;
    }

    public void setTrabajoDefinicion(TrabajoDefinicion trabajoDefinicion) {
        this.trabajoDefinicion = trabajoDefinicion;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Usuario estudiante) {
        this.estudiante = estudiante;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public Integer getPuntosObtenidos() {
        return puntosObtenidos;
    }

    public void setPuntosObtenidos(Integer puntosObtenidos) {
        this.puntosObtenidos = puntosObtenidos;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
