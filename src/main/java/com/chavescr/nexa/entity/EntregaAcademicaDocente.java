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
import jakarta.persistence.UniqueConstraint;

/** Respuesta de un docente a una {@link EntregaAcademica}: borrador, presentación y revisión de la dirección. */
@Entity
@Table(name = "entregas_academicas_docente", uniqueConstraints = @UniqueConstraint(
        name = "uk_entrega_academica_docente", columnNames = { "entrega_id", "docente_id" }))
public class EntregaAcademicaDocente {

    public enum EstadoPresentacion {
        BORRADOR("Borrador", "borrador"),
        PRESENTADA("Presentada", "presentada"),
        DEVUELTA("Devuelta", "devuelta"),
        APROBADA("Aprobada", "aprobada");

        private final String etiqueta;
        private final String css;

        EstadoPresentacion(String etiqueta, String css) {
            this.etiqueta = etiqueta;
            this.css = css;
        }

        public String getEtiqueta() { return etiqueta; }
        public String getCss() { return css; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private EntregaAcademica entrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Usuario docente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoPresentacion estado = EstadoPresentacion.BORRADOR;

    @Column(length = 2000)
    private String observacion;

    @Column(length = 1000)
    private String observacionRevision;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaPresentacion;

    private LocalDateTime fechaRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;

    @OneToMany(mappedBy = "presentacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCreacion DESC, id DESC")
    private List<AdjuntoEntregaAcademica> adjuntos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    /** El docente puede seguir editando mientras es borrador o la dirección la devolvió. */
    public boolean isEditable() {
        return estado == EstadoPresentacion.BORRADOR || estado == EstadoPresentacion.DEVUELTA;
    }

    /** Se presentó pero la dirección todavía no la revisa. */
    public boolean isPorRevisar() {
        return estado == EstadoPresentacion.PRESENTADA;
    }

    /** Presentada o aprobada: ya no está pendiente para el docente. */
    public boolean isRealizada() {
        return estado == EstadoPresentacion.PRESENTADA || estado == EstadoPresentacion.APROBADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EntregaAcademica getEntrega() { return entrega; }
    public void setEntrega(EntregaAcademica entrega) { this.entrega = entrega; }
    public Usuario getDocente() { return docente; }
    public void setDocente(Usuario docente) { this.docente = docente; }
    public EstadoPresentacion getEstado() { return estado; }
    public void setEstado(EstadoPresentacion estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaPresentacion() { return fechaPresentacion; }
    public void setFechaPresentacion(LocalDateTime fechaPresentacion) { this.fechaPresentacion = fechaPresentacion; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }
    public Usuario getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(Usuario revisadoPor) { this.revisadoPor = revisadoPor; }
    public List<AdjuntoEntregaAcademica> getAdjuntos() { return adjuntos; }
    public void setAdjuntos(List<AdjuntoEntregaAcademica> adjuntos) { this.adjuntos = adjuntos; }
}
