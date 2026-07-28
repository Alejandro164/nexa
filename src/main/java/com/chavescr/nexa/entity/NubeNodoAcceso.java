package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "nube_nodo_accesos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nube_acceso_nodo_usuario", columnNames = { "nodo_id", "usuario_id" })
})
public class NubeNodoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nodo_id", nullable = false)
    private NubeNodo nodo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelAcceso nivel = NivelAcceso.LECTOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compartido_por_id")
    private Usuario compartidoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCompartido;

    public enum NivelAcceso {
        LECTOR, EDITOR
    }

    @PrePersist
    protected void onCreate() {
        this.fechaCompartido = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NubeNodo getNodo() {
        return nodo;
    }

    public void setNodo(NubeNodo nodo) {
        this.nodo = nodo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public NivelAcceso getNivel() {
        return nivel;
    }

    public void setNivel(NivelAcceso nivel) {
        this.nivel = nivel;
    }

    public Usuario getCompartidoPor() {
        return compartidoPor;
    }

    public void setCompartidoPor(Usuario compartidoPor) {
        this.compartidoPor = compartidoPor;
    }

    public LocalDateTime getFechaCompartido() {
        return fechaCompartido;
    }

    public void setFechaCompartido(LocalDateTime fechaCompartido) {
        this.fechaCompartido = fechaCompartido;
    }
}
