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
import jakarta.persistence.UniqueConstraint;

/**
 * Conversación directa (1 a 1) dentro de una dirección. El par se guarda ordenado por id
 * ({@code usuarioA.id < usuarioB.id}) para que exista una sola conversación por pareja.
 * El último mensaje leído de cada lado vive aquí, así el conteo de "no leídos" no necesita
 * una tabla de lecturas por mensaje.
 */
@Entity
@Table(name = "mensajeria_conversaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_mensajeria_conversacion_par",
                columnNames = { "direccion_id", "usuario_a_id", "usuario_b_id" }),
        indexes = {
                @Index(name = "idx_mensajeria_conversacion_a", columnList = "usuario_a_id"),
                @Index(name = "idx_mensajeria_conversacion_b", columnList = "usuario_b_id")
        })
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_a_id", nullable = false)
    private Usuario usuarioA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_b_id", nullable = false)
    private Usuario usuarioB;

    @Column(name = "ultimo_leido_a", nullable = false)
    private Long ultimoLeidoA = 0L;

    @Column(name = "ultimo_leido_b", nullable = false)
    private Long ultimoLeidoB = 0L;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_ultimo_mensaje", nullable = false)
    private LocalDateTime fechaUltimoMensaje;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaCreacion = ahora;
        if (this.fechaUltimoMensaje == null) {
            this.fechaUltimoMensaje = ahora;
        }
    }

    public boolean participa(Long usuarioId) {
        return usuarioA.getId().equals(usuarioId) || usuarioB.getId().equals(usuarioId);
    }

    public Usuario getOtro(Long usuarioId) {
        return usuarioA.getId().equals(usuarioId) ? usuarioB : usuarioA;
    }

    public Long getUltimoLeido(Long usuarioId) {
        return usuarioA.getId().equals(usuarioId) ? ultimoLeidoA : ultimoLeidoB;
    }

    /** Solo avanza: abrir una conversación nunca "desmarca" mensajes ya leídos. */
    public void marcarLeidoHasta(Long usuarioId, Long mensajeId) {
        if (usuarioA.getId().equals(usuarioId)) {
            ultimoLeidoA = Math.max(ultimoLeidoA, mensajeId);
        } else {
            ultimoLeidoB = Math.max(ultimoLeidoB, mensajeId);
        }
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

    public Usuario getUsuarioA() {
        return usuarioA;
    }

    public void setUsuarioA(Usuario usuarioA) {
        this.usuarioA = usuarioA;
    }

    public Usuario getUsuarioB() {
        return usuarioB;
    }

    public void setUsuarioB(Usuario usuarioB) {
        this.usuarioB = usuarioB;
    }

    public Long getUltimoLeidoA() {
        return ultimoLeidoA;
    }

    public void setUltimoLeidoA(Long ultimoLeidoA) {
        this.ultimoLeidoA = ultimoLeidoA;
    }

    public Long getUltimoLeidoB() {
        return ultimoLeidoB;
    }

    public void setUltimoLeidoB(Long ultimoLeidoB) {
        this.ultimoLeidoB = ultimoLeidoB;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaUltimoMensaje() {
        return fechaUltimoMensaje;
    }

    public void setFechaUltimoMensaje(LocalDateTime fechaUltimoMensaje) {
        this.fechaUltimoMensaje = fechaUltimoMensaje;
    }
}
