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

/**
 * Archivo o enlace adjunto a una publicación (lo sube el docente) o a una entrega (lo sube el
 * estudiante). Exactamente uno de {@code publicacion}/{@code entrega} está definido, y
 * exactamente uno de {@code ruta} (archivo en disco) / {@code url} (enlace externo).
 */
@Entity
@Table(name = "classroom_adjuntos")
public class ClassroomAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id")
    private ClassroomPublicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id")
    private ClassroomEntrega entrega;

    @Column(nullable = false, length = 255)
    private String nombre;

    // Relativa a ruta.recursos: <cédula>/classroom/<claseId>/...
    @Column(length = 500)
    private String ruta;

    @Column(length = 1000)
    private String url;

    @Column(length = 20)
    private String extension;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public boolean isEnlace() {
        return url != null;
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

    public ClassroomEntrega getEntrega() {
        return entrega;
    }

    public void setEntrega(ClassroomEntrega entrega) {
        this.entrega = entrega;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
