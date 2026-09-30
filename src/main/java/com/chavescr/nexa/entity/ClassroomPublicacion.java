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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Anuncio del tablón, tarea o material de una clase del Classroom.
 * Una tarea calificable queda vinculada a un {@link Componente} de evaluación (Tarea, Proyecto,
 * Prueba o Cotidiano) de la sección y materia, y sus notas se guardan como ResultadoComponente:
 * así aparecen en Gestión Académica y en los promedios sin duplicar datos.
 */
@Entity
@Table(name = "classroom_publicaciones")
public class ClassroomPublicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private ClassroomClase clase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPublicacion tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(length = 200)
    private String titulo;

    @Column(length = 5000)
    private String contenido;

    // Agrupa tareas y materiales (ej. "Unidad 1"); vacío = sin tema
    @Column(length = 100)
    private String tema;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "puntos_totales")
    private Integer puntosTotales;

    // Id simple (sin llave foránea, como Componente.origenId): si el componente se elimina desde
    // Gestión Académica, la tarea sigue existiendo y solo deja de ser calificable.
    @Column(name = "componente_id")
    private Long componenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "clave_componente", length = 20)
    private ClaveComponente claveComponente;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ClassroomAdjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<ClassroomComentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassroomEntrega> entregas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public boolean isCalificable() {
        return componenteId != null;
    }

    public boolean isVencida() {
        return fechaEntrega != null && LocalDateTime.now().isAfter(fechaEntrega);
    }

    // Mismos nombres que usa Gestión Académica para cada rubro
    public String getEtiquetaRubro() {
        if (claveComponente == null) {
            return null;
        }
        return switch (claveComponente) {
            case TAREA -> "Tarea";
            case PROYECTO -> "Proyecto";
            case EXAMEN -> "Prueba";
            case COTIDIANO -> "Trabajo cotidiano";
        };
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassroomClase getClase() {
        return clase;
    }

    public void setClase(ClassroomClase clase) {
        this.clase = clase;
    }

    public TipoPublicacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoPublicacion tipo) {
        this.tipo = tipo;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Integer getPuntosTotales() {
        return puntosTotales;
    }

    public void setPuntosTotales(Integer puntosTotales) {
        this.puntosTotales = puntosTotales;
    }

    public Long getComponenteId() {
        return componenteId;
    }

    public void setComponenteId(Long componenteId) {
        this.componenteId = componenteId;
    }

    public ClaveComponente getClaveComponente() {
        return claveComponente;
    }

    public void setClaveComponente(ClaveComponente claveComponente) {
        this.claveComponente = claveComponente;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEdicion() {
        return fechaEdicion;
    }

    public void setFechaEdicion(LocalDateTime fechaEdicion) {
        this.fechaEdicion = fechaEdicion;
    }

    public List<ClassroomAdjunto> getAdjuntos() {
        return adjuntos;
    }

    public List<ClassroomComentario> getComentarios() {
        return comentarios;
    }

    public List<ClassroomEntrega> getEntregas() {
        return entregas;
    }
}
