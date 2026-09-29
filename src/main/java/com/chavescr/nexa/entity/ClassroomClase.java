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

/**
 * Clase del Classroom: una materia impartida a una sección. No se crea a mano; se materializa
 * a partir del horario (docente + materia + sección) la primera vez que alguien la ve, y sus
 * miembros (docentes y estudiantes) se derivan siempre del horario y de la sección.
 */
@Entity
@Table(name = "classroom_clases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_classroom_clase_direccion_materia_nivel",
                columnNames = { "direccion_id", "materia_id", "nivel_id" })
})
public class ClassroomClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelAcademico nivel;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getSeccion() {
        return nivel.getGrado() + "-" + nivel.getSeccion();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public NivelAcademico getNivel() {
        return nivel;
    }

    public void setNivel(NivelAcademico nivel) {
        this.nivel = nivel;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
