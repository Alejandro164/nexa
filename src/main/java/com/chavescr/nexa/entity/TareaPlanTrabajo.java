package com.chavescr.nexa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tareas_plan_trabajo")
public class TareaPlanTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    /** Fecha de inicio opcional; la fecha límite es el vencimiento. */
    @Column(nullable = true)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'NINGUNO'")
    private RecordatorioTarea recordatorio = RecordatorioTarea.NINGUNO;

    /** true cuando ya se avisó del recordatorio actual; se reinicia al cambiar la fecha límite o el recordatorio. */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean recordatorioEnviado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    @Column(nullable = true)
    private Integer calificacion;

    @Column(nullable = true)
    private LocalDateTime fechaCompletada;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "miembro_id", nullable = true)
    private MiembroPlanTrabajo miembro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_trabajo_id", nullable = false)
    private PlanTrabajo planTrabajo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadTarea prioridad = PrioridadTarea.MEDIA;

    /** Colaboradores adicionales al responsable ({@link #miembro}). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tareas_plan_trabajo_colaboradores",
            joinColumns = @JoinColumn(name = "tarea_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    private Set<Usuario> colaboradores = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tareas_plan_trabajo_etiquetas",
            joinColumns = @JoinColumn(name = "tarea_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    private Set<EtiquetaTarea> etiquetas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    private List<ChecklistItemTarea> checklist = new ArrayList<>();

    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCreacion DESC, id DESC")
    private List<AdjuntoTarea> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha DESC, id DESC")
    private List<ActividadTarea> actividad = new ArrayList<>();

    /**
     * Flujo de trabajo de una tarea. Es un enum (no una tabla) para no cargar la base de datos;
     * agregar un estado es agregar una constante aquí y ampliar la restricción CHECK de la columna.
     */
    public enum EstadoTarea {
        PENDIENTE("Pendiente", "Backlog / Entrada",
                "Tareas asignadas, circulares recibidas, requerimientos de planeamiento y solicitudes administrativas pendientes de iniciar."),
        EN_EJECUCION("En ejecución", "Trabajo activo (WIP)",
                "Procesos en desarrollo activo por parte del docente (redacción de planeamiento, revisión de evaluaciones, preparación de material)."),
        EN_REVISION("En revisión", "Control y auditoría",
                "El docente entrega el insumo y queda en revisión de dirección, comité pedagógico o coordinación técnica."),
        EN_CORRECCION("En corrección", "Bucle de mejora",
                "Tareas que requieren correcciones curriculares, ajustes administrativos o justificación formal según observaciones de la dirección."),
        FINALIZADO("Finalizado", "Cierre formal",
                "Entregables completamente validados con visto bueno de dirección, archivo de evidencias y cumplimiento curricular.");

        private final String etiqueta;
        private final String fase;
        private final String descripcion;

        EstadoTarea(String etiqueta, String fase, String descripcion) {
            this.etiqueta = etiqueta;
            this.fase = fase;
            this.descripcion = descripcion;
        }

        public String getEtiqueta() { return etiqueta; }
        public String getFase() { return fase; }
        public String getDescripcion() { return descripcion; }

        /** Clase CSS del chip: en-ejecucion, en-revision... */
        public String getCss() { return name().toLowerCase().replace('_', '-'); }

        public boolean isCerrado() { return this == FINALIZADO; }
    }

    /** Cuándo avisar antes del vencimiento (el aviso llega a la campanita del responsable y los colaboradores). */
    public enum RecordatorioTarea {
        NINGUNO("Ninguno", -1),
        AL_VENCER("Al vencer", 0),
        UN_DIA_ANTES("1 día antes", 1),
        DOS_DIAS_ANTES("2 días antes", 2),
        UNA_SEMANA_ANTES("1 semana antes", 7);

        private final String etiqueta;
        private final int diasAntes;

        RecordatorioTarea(String etiqueta, int diasAntes) {
            this.etiqueta = etiqueta;
            this.diasAntes = diasAntes;
        }

        public String getEtiqueta() { return etiqueta; }
        public int getDiasAntes() { return diasAntes; }
    }

    public enum PrioridadTarea {
        ALTA, MEDIA, BAJA
    }

    public PrioridadTarea getPrioridad() { return prioridad; }
    public void setPrioridad(PrioridadTarea prioridad) { this.prioridad = prioridad; }

    @PrePersist
    protected void onCreate() {
        this.fechaAsignacion = LocalDateTime.now();
    }

    /** Pasada la fecha límite sin haberse finalizado (se calcula; ya no es un estado guardado). */
    public boolean isVencida() {
        return estado != null && !estado.isCerrado() && fechaLimite != null && fechaLimite.isBefore(LocalDate.now());
    }

    public int getChecklistTotal() { return checklist.size(); }

    public long getChecklistCompletados() {
        return checklist.stream().filter(ChecklistItemTarea::isCompletado).count();
    }

    public int getChecklistPorcentaje() {
        return checklist.isEmpty() ? 0 : (int) Math.round(getChecklistCompletados() * 100.0 / checklist.size());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public RecordatorioTarea getRecordatorio() { return recordatorio; }
    public void setRecordatorio(RecordatorioTarea recordatorio) { this.recordatorio = recordatorio; }
    public boolean isRecordatorioEnviado() { return recordatorioEnviado; }
    public void setRecordatorioEnviado(boolean recordatorioEnviado) { this.recordatorioEnviado = recordatorioEnviado; }
    public Set<Usuario> getColaboradores() { return colaboradores; }
    public void setColaboradores(Set<Usuario> colaboradores) { this.colaboradores = colaboradores; }
    public Set<EtiquetaTarea> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(Set<EtiquetaTarea> etiquetas) { this.etiquetas = etiquetas; }
    public List<ChecklistItemTarea> getChecklist() { return checklist; }
    public void setChecklist(List<ChecklistItemTarea> checklist) { this.checklist = checklist; }
    public List<AdjuntoTarea> getAdjuntos() { return adjuntos; }
    public void setAdjuntos(List<AdjuntoTarea> adjuntos) { this.adjuntos = adjuntos; }
    public List<ActividadTarea> getActividad() { return actividad; }
    public void setActividad(List<ActividadTarea> actividad) { this.actividad = actividad; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public EstadoTarea getEstado() { return estado; }
    public void setEstado(EstadoTarea estado) { this.estado = estado; }
    public Integer getCalificacion() { return calificacion; }
    public void setCalificacion(Integer calificacion) { this.calificacion = calificacion; }
    public LocalDateTime getFechaCompletada() { return fechaCompletada; }
    public void setFechaCompletada(LocalDateTime fechaCompletada) { this.fechaCompletada = fechaCompletada; }
    public MiembroPlanTrabajo getMiembro() { return miembro; }
    public void setMiembro(MiembroPlanTrabajo miembro) { this.miembro = miembro; }
    public PlanTrabajo getPlanTrabajo() { return planTrabajo; }
    public void setPlanTrabajo(PlanTrabajo planTrabajo) { this.planTrabajo = planTrabajo; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
}
