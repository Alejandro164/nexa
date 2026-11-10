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

/**
 * Componente de evaluación de una dirección: cuánto vale cada rubro (trabajo cotidiano, tareas, proyecto,
 * pruebas y asistencia) en la nota del período. Cada materia tiene uno asignado y toma sus porcentajes;
 * al cambiar un porcentaje, todas las materias del componente se calculan con el valor nuevo.
 * Reemplaza a la antigua distribución porcentual por materia y período.
 */
@Entity
@Table(name = "componentes_evaluacion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_componente_evaluacion_direccion_nombre", columnNames = { "direccion_id", "nombre" })
})
public class ComponenteEvaluacion {

    /** Pesos que se usan si una materia no tuviera componente: cotidiano, tareas, proyecto, pruebas, asistencia. */
    public static final int[] PESOS_POR_DEFECTO = { 40, 15, 20, 20, 5 };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @Column(nullable = false, length = 60)
    private String nombre;

    /** Referencia al reglamento o a qué asignaturas aplica, por ejemplo "Art. 41 a)". */
    @Column(length = 160)
    private String descripcion;

    @Column(nullable = false)
    private Integer cotidiano = 0;

    @Column(nullable = false)
    private Integer tareas = 0;

    @Column(nullable = false)
    private Integer proyecto = 0;

    @Column(nullable = false)
    private Integer pruebas = 0;

    @Column(nullable = false)
    private Integer asistencia = 0;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    /** Pesos en el orden que usan los cálculos de notas: cotidiano, tareas, proyecto, pruebas, asistencia. */
    public int[] pesos() {
        return new int[] { valor(cotidiano), valor(tareas), valor(proyecto), valor(pruebas), valor(asistencia) };
    }

    public int getTotal() {
        return valor(cotidiano) + valor(tareas) + valor(proyecto) + valor(pruebas) + valor(asistencia);
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public Integer getProyecto() {
        return proyecto;
    }

    public void setProyecto(Integer proyecto) {
        this.proyecto = proyecto;
    }

    public Integer getPruebas() {
        return pruebas;
    }

    public void setPruebas(Integer pruebas) {
        this.pruebas = pruebas;
    }

    public Integer getAsistencia() {
        return asistencia;
    }

    public void setAsistencia(Integer asistencia) {
        this.asistencia = asistencia;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
