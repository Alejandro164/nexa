package com.chavescr.nexa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Destinatario interno de un {@link Oficio}. Según el {@link TipoDestinatario} se usa {@code usuario}
 * (PERSONA) o {@code nivel} (FAMILIAS_SECCION); los demás grupos no llevan ninguno de los dos.
 */
@Entity
@Table(name = "oficio_destinatarios", indexes = @Index(name = "idx_oficio_destinatario_oficio", columnList = "oficio_id"))
public class OficioDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oficio_id", nullable = false)
    private Oficio oficio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDestinatario tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")
    private NivelAcademico nivel;

    public OficioDestinatario() {
    }

    public OficioDestinatario(Oficio oficio, TipoDestinatario tipo, Usuario usuario, NivelAcademico nivel) {
        this.oficio = oficio;
        this.tipo = tipo;
        this.usuario = usuario;
        this.nivel = nivel;
    }

    /** Identificador estable para agrupar y para el formulario: "COMUNIDAD", "SECCION-12", "PERSONA-50". */
    public String clave() {
        return switch (tipo) {
            case PERSONA -> "PERSONA-" + usuario.getId();
            case FAMILIAS_SECCION -> "SECCION-" + nivel.getId();
            default -> tipo.name();
        };
    }

    public String etiqueta() {
        return switch (tipo) {
            case PERSONA -> usuario.getNombre();
            case FAMILIAS_SECCION -> "Familias de " + nivel.getNombreCompleto();
            default -> tipo.getEtiqueta();
        };
    }

    public Long getId() {
        return id;
    }

    public Oficio getOficio() {
        return oficio;
    }

    public void setOficio(Oficio oficio) {
        this.oficio = oficio;
    }

    public TipoDestinatario getTipo() {
        return tipo;
    }

    public void setTipo(TipoDestinatario tipo) {
        this.tipo = tipo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public NivelAcademico getNivel() {
        return nivel;
    }

    public void setNivel(NivelAcademico nivel) {
        this.nivel = nivel;
    }
}
