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

/**
 * Entrega académica que la dirección le pide a su personal docente (planeamiento, informes, etc.),
 * con un plazo de presentación. Cada docente la responde con una {@link EntregaAcademicaDocente}.
 */
@Entity
@Table(name = "entregas_academicas")
public class EntregaAcademica {

    public enum TipoEntrega {
        PLANEAMIENTO("Planeamiento didáctico"),
        INFORME("Informe"),
        EVALUACION("Evaluación"),
        ADMINISTRATIVA("Administrativa"),
        OTRA("Otra");

        private final String etiqueta;

        TipoEntrega(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() { return etiqueta; }
    }

    /** Situación del plazo de presentación respecto a hoy. */
    public enum Plazo {
        PROXIMA("Próxima", "plazo-proxima"),
        EN_PLAZO("En plazo", "plazo-en-plazo"),
        VENCIDA("Fuera de plazo", "plazo-vencida");

        private final String etiqueta;
        private final String css;

        Plazo(String etiqueta, String css) {
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
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEntrega tipo = TipoEntrega.OTRA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    private PeriodoAcademico periodo;

    /** Primer día en que los docentes ven la entrega como pendiente. */
    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public Plazo getPlazo() {
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(fechaInicio)) {
            return Plazo.PROXIMA;
        }
        return hoy.isAfter(fechaFin) ? Plazo.VENCIDA : Plazo.EN_PLAZO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Direccion getDireccion() { return direccion; }
    public void setDireccion(Direccion direccion) { this.direccion = direccion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoEntrega getTipo() { return tipo; }
    public void setTipo(TipoEntrega tipo) { this.tipo = tipo; }
    public PeriodoAcademico getPeriodo() { return periodo; }
    public void setPeriodo(PeriodoAcademico periodo) { this.periodo = periodo; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public Usuario getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Usuario creadoPor) { this.creadoPor = creadoPor; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
