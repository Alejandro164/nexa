package com.chavescr.nexa.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Trabajo de un estudiante sobre una tarea. La calificación no se guarda aquí sino en el
 * ResultadoComponente del componente vinculado a la tarea (fuente única de la nota).
 */
@Entity
@Table(name = "classroom_entregas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_classroom_entrega_publicacion_estudiante",
                columnNames = { "publicacion_id", "estudiante_id" })
})
public class ClassroomEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private ClassroomPublicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEntrega estado = EstadoEntrega.ASIGNADA;

    @Column(length = 5000)
    private String texto;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;

    @Column(name = "comentario_docente", length = 500)
    private String comentarioDocente;

    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ClassroomAdjunto> adjuntos = new ArrayList<>();

    public boolean isTarde() {
        LocalDateTime limite = publicacion.getFechaEntrega();
        return fechaEntrega != null && limite != null && fechaEntrega.isAfter(limite);
    }

    public Long getId() {
        return id;
    }

    public ClassroomPublicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(ClassroomPublicacion publicacion) {
        this.publicacion = publicacion;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Usuario estudiante) {
        this.estudiante = estudiante;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }

    public void setEstado(EstadoEntrega estado) {
        this.estado = estado;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getComentarioDocente() {
        return comentarioDocente;
    }

    public void setComentarioDocente(String comentarioDocente) {
        this.comentarioDocente = comentarioDocente;
    }

    public List<ClassroomAdjunto> getAdjuntos() {
        return adjuntos;
    }
}
