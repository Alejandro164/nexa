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
@Table(name = "docente_bloqueo_lecciones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bloqueo_docente_periodo_dia_leccion",
                columnNames = { "institucion_id", "docente_id", "periodo_id", "dia", "numero_leccion" })
})
public class DocenteBloqueoLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Usuario docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    @Column(nullable = false, length = 12)
    private String dia;

    @Column(name = "numero_leccion", nullable = false)
    private Integer numeroLeccion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Institucion getInstitucion() { return institucion; }
    public void setInstitucion(Institucion institucion) { this.institucion = institucion; }
    public Usuario getDocente() { return docente; }
    public void setDocente(Usuario docente) { this.docente = docente; }
    public PeriodoAcademico getPeriodo() { return periodo; }
    public void setPeriodo(PeriodoAcademico periodo) { this.periodo = periodo; }
    public String getDia() { return dia; }
    public void setDia(String dia) { this.dia = dia; }
    public Integer getNumeroLeccion() { return numeroLeccion; }
    public void setNumeroLeccion(Integer numeroLeccion) { this.numeroLeccion = numeroLeccion; }
}
