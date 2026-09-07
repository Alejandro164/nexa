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
@Table(name = "distribuciones_porcentuales", uniqueConstraints = {
        @UniqueConstraint(name = "uk_distribucion_periodo_materia", columnNames = { "periodo_id", "materia_id" })
})
public class DistribucionPorcentual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(nullable = false)
    private Integer cotidiano = 0;

    @Column(nullable = false)
    private Integer tareas = 0;

    @Column(nullable = false)
    private Integer proyectos = 0;

    @Column(nullable = false)
    private Integer examenes = 0;

    @Column(nullable = false)
    private Integer asistencia = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Institucion getInstitucion() {
        return institucion;
    }

    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoAcademico periodo) {
        this.periodo = periodo;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public Integer getCotidiano() {
        return cotidiano;
    }

    public void setCotidiano(Integer cotidiano) {
        this.cotidiano = cotidiano;
    }

    public Integer getTareas() {
        return tareas;
    }

    public void setTareas(Integer tareas) {
        this.tareas = tareas;
    }

    public Integer getProyectos() {
        return proyectos;
    }

    public void setProyectos(Integer proyectos) {
        this.proyectos = proyectos;
    }

    public Integer getExamenes() {
        return examenes;
    }

    public void setExamenes(Integer examenes) {
        this.examenes = examenes;
    }

    public Integer getAsistencia() {
        return asistencia;
    }

    public void setAsistencia(Integer asistencia) {
        this.asistencia = asistencia;
    }

    public int getTotal() {
        return cotidiano + tareas + proyectos + examenes + asistencia;
    }
}
