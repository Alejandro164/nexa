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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Incidencia disciplinaria de un estudiante (llamada de atención o boleta).
 * Solo las boletas restan {@link #puntosDescontados} de 100.
 * Las llamadas de atención se registran para seguimiento y no bajan la nota.
 */
@Entity
@Table(name = "incidentes_conducta", indexes = {
        @Index(name = "idx_incidente_conducta_inst_periodo", columnList = "institucion_id, periodo_id"),
        @Index(name = "idx_incidente_conducta_inst_periodo_tipo", columnList = "institucion_id, periodo_id, tipo"),
        @Index(name = "idx_incidente_conducta_estudiante_periodo",
                columnList = "institucion_id, periodo_id, estudiante_id")
})
public class IncidenteConducta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoIncidente tipo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(length = 2000)
    private String descripcion;

    @Column(name = "puntos_descontados", nullable = false)
    private Integer puntosDescontados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoIncidente estado = EstadoIncidente.PENDIENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public enum TipoIncidente {
        LLAMADA_ATENCION(false),
        BOLETA(true);

        private final boolean afectaNota;

        TipoIncidente(boolean afectaNota) {
            this.afectaNota = afectaNota;
        }

        public boolean afectaNota() {
            return afectaNota;
        }
    }

    public enum EstadoIncidente {
        PENDIENTE, EN_PROCESO, RESUELTO, APELADO
    }

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoIncidente.PENDIENTE;
        }
        if (this.puntosDescontados == null && this.tipo != null && !this.tipo.afectaNota()) {
            this.puntosDescontados = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Institucion getInstitucion() {
        return institucion;
    }

    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }

    public PeriodoAcademico getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoAcademico periodo) {
        this.periodo = periodo;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Usuario estudiante) {
        this.estudiante = estudiante;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    public TipoIncidente getTipo() {
        return tipo;
    }

    public void setTipo(TipoIncidente tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getPuntosDescontados() {
        return puntosDescontados;
    }

    public void setPuntosDescontados(Integer puntosDescontados) {
        this.puntosDescontados = puntosDescontados;
    }

    public EstadoIncidente getEstado() {
        return estado;
    }

    public void setEstado(EstadoIncidente estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
