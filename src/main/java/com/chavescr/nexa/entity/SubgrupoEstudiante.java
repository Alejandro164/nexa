package com.chavescr.nexa.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * En una lección partida, el estudiante asiste a una sola de las materias del horario.
 * La lección ya trae materia, docente, aula, período y sección.
 */
@Entity
@Table(name = "subgrupo_estudiantes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subgrupo_estudiante_leccion", columnNames = { "estudiante_id", "horario_leccion_id" })
})
public class SubgrupoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_leccion_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HorarioLeccion leccion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Usuario estudiante) {
        this.estudiante = estudiante;
    }

    public HorarioLeccion getLeccion() {
        return leccion;
    }

    public void setLeccion(HorarioLeccion leccion) {
        this.leccion = leccion;
    }
}
