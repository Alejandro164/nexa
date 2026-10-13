package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Mensaje de una {@link Conversacion}: texto, un archivo adjunto (documento o foto), o ambos. */
@Entity
@Table(name = "mensajeria_mensajes",
        indexes = @Index(name = "idx_mensajeria_mensaje_conversacion", columnList = "conversacion_id, id"))
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(length = 4000)
    private String texto;

    /** Mensaje al que se responde (siempre de la misma conversación). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respuesta_a_id")
    private Mensaje respuestaA;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    /** Ruta relativa a {@code ruta.recursos}; null si el mensaje no lleva archivo. */
    @Column(name = "archivo_ruta", length = 500)
    private String archivoRuta;

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    @Column(name = "archivo_tipo_contenido", length = 100)
    private String archivoTipoContenido;

    @Column(name = "archivo_tamano")
    private Long archivoTamano;

    @Column(name = "archivo_imagen")
    private Boolean archivoImagen = false;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }

    public boolean tieneArchivo() {
        return archivoRuta != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Conversacion getConversacion() {
        return conversacion;
    }

    public void setConversacion(Conversacion conversacion) {
        this.conversacion = conversacion;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Mensaje getRespuestaA() {
        return respuestaA;
    }

    public void setRespuestaA(Mensaje respuestaA) {
        this.respuestaA = respuestaA;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getArchivoRuta() {
        return archivoRuta;
    }

    public void setArchivoRuta(String archivoRuta) {
        this.archivoRuta = archivoRuta;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public void setArchivoNombre(String archivoNombre) {
        this.archivoNombre = archivoNombre;
    }

    public String getArchivoTipoContenido() {
        return archivoTipoContenido;
    }

    public void setArchivoTipoContenido(String archivoTipoContenido) {
        this.archivoTipoContenido = archivoTipoContenido;
    }

    public Long getArchivoTamano() {
        return archivoTamano;
    }

    public void setArchivoTamano(Long archivoTamano) {
        this.archivoTamano = archivoTamano;
    }

    public Boolean getArchivoImagen() {
        return archivoImagen;
    }

    public void setArchivoImagen(Boolean archivoImagen) {
        this.archivoImagen = archivoImagen;
    }
}
