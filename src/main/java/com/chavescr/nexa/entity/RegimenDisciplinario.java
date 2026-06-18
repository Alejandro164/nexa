package com.chavescr.nexa.entity;

import java.time.LocalDate;
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

@Entity
@Table(name = "regimen_disciplinario")
public class RegimenDisciplinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoRegimen tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Usuario funcionario;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoRegimen estado = EstadoRegimen.PENDIENTE;

    @Column(length = 2000)
    private String resolucion;

    private LocalDate fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public enum TipoRegimen {
        LLAMADA_ATENCION, AMONESTACION, PROCESO_DISCIPLINARIO
    }

    public enum EstadoRegimen {
        PENDIENTE, EN_PROCESO, RESUELTO, APELADO
    }

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoRegimen getTipo() { return tipo; }
    public void setTipo(TipoRegimen tipo) { this.tipo = tipo; }
    public Usuario getFuncionario() { return funcionario; }
    public void setFuncionario(Usuario funcionario) { this.funcionario = funcionario; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public EstadoRegimen getEstado() { return estado; }
    public void setEstado(EstadoRegimen estado) { this.estado = estado; }
    public String getResolucion() { return resolucion; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }
    public LocalDate getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDate fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public Institucion getInstitucion() { return institucion; }
    public void setInstitucion(Institucion institucion) { this.institucion = institucion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
