package com.chavescr.nexa.entity;

import java.time.LocalDateTime;
import java.util.Locale;

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

/** Archivo o enlace adjunto a la respuesta de un docente: exactamente uno de {@code ruta} (archivo) o {@code url}. */
@Entity
@Table(name = "adjuntos_entrega_academica")
public class AdjuntoEntregaAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentacion_id", nullable = false)
    private EntregaAcademicaDocente presentacion;

    @Column(nullable = false, length = 255)
    private String nombre;

    // Relativa a ruta.recursos: <cédula>/entregas-academicas/<entregaId>/<docenteId>/...
    @Column(length = 500)
    private String ruta;

    @Column(length = 1000)
    private String url;

    @Column(length = 20)
    private String extension;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public boolean isEnlace() { return url != null; }

    /** Tamaño legible: "340 KB", "1.2 MB". */
    public String getTamanoLegible() {
        if (tamanoBytes == null) return "";
        if (tamanoBytes < 1024) return tamanoBytes + " B";
        if (tamanoBytes < 1024 * 1024) return Math.round(tamanoBytes / 1024.0) + " KB";
        return String.format(Locale.US, "%.1f MB", tamanoBytes / (1024.0 * 1024.0));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EntregaAcademicaDocente getPresentacion() { return presentacion; }
    public void setPresentacion(EntregaAcademicaDocente presentacion) { this.presentacion = presentacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
