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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "miembros_plan_trabajo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_miembro_plan_trabajo_usuario", columnNames = {"plan_trabajo_id", "usuario_id"})
})
public class MiembroPlanTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_trabajo_id", nullable = false)
    private PlanTrabajo planTrabajo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RolPlanTrabajo rol = RolPlanTrabajo.MIEMBRO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @OneToMany(mappedBy = "miembro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TareaPlanTrabajo> tareas = new ArrayList<>();

    public enum RolPlanTrabajo {
        LIDER, MIEMBRO
    }

    @PrePersist
    protected void onCreate() {
        this.fechaAsignacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlanTrabajo getPlanTrabajo() { return planTrabajo; }
    public void setPlanTrabajo(PlanTrabajo planTrabajo) { this.planTrabajo = planTrabajo; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public RolPlanTrabajo getRol() { return rol; }
    public void setRol(RolPlanTrabajo rol) { this.rol = rol; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
    public List<TareaPlanTrabajo> getTareas() { return tareas; }
    public void setTareas(List<TareaPlanTrabajo> tareas) { this.tareas = tareas; }
}
