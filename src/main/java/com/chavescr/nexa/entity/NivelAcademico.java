package com.chavescr.nexa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "niveles_academicos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nivel_direccion_grado_seccion",
                columnNames = {"direccion_id", "grado", "seccion"})
})
public class NivelAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @Column(nullable = false)
    private Integer grado;

    @Column(nullable = false, length = 20)
    private String seccion;

    /** Define qué escala de notas usan sus estudiantes. Nullable en el esquema solo para la migración. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CicloEducativo ciclo;

    @Column(nullable = false)
    private Boolean activo = true;

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

    public Integer getGrado() {
        return grado;
    }

    public void setGrado(Integer grado) {
        this.grado = grado;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public CicloEducativo getCiclo() {
        return ciclo;
    }

    public void setCiclo(CicloEducativo ciclo) {
        this.ciclo = ciclo;
    }

    /** El ciclo guardado o, si todavía no tiene, el que corresponde a su grado. */
    public CicloEducativo getCicloEfectivo() {
        return ciclo != null ? ciclo : CicloEducativo.deGrado(grado);
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getNombreCompleto() {
        return grado + " - " + seccion;
    }
}
