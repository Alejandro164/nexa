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
@Table(name = "seccion_bloqueo_lecciones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bloqueo_seccion_periodo_dia_leccion",
                columnNames = { "direccion_id", "nivel_id", "periodo_id", "dia", "numero_leccion" })
})
public class SeccionBloqueoLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelAcademico nivel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    @Column(nullable = false, length = 12)
    private String dia;

    @Column(name = "numero_leccion", nullable = false)
    private Integer numeroLeccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_materia_id", nullable = false)
    private TipoMateria tipoMateria;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Direccion getDireccion() { return direccion; }
    public void setDireccion(Direccion direccion) { this.direccion = direccion; }
    public NivelAcademico getNivel() { return nivel; }
    public void setNivel(NivelAcademico nivel) { this.nivel = nivel; }
    public PeriodoAcademico getPeriodo() { return periodo; }
    public void setPeriodo(PeriodoAcademico periodo) { this.periodo = periodo; }
    public String getDia() { return dia; }
    public void setDia(String dia) { this.dia = dia; }
    public Integer getNumeroLeccion() { return numeroLeccion; }
    public void setNumeroLeccion(Integer numeroLeccion) { this.numeroLeccion = numeroLeccion; }
    public TipoMateria getTipoMateria() { return tipoMateria; }
    public void setTipoMateria(TipoMateria tipoMateria) { this.tipoMateria = tipoMateria; }
}
