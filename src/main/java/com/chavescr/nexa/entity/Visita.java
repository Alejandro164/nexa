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

@Entity
@Table(name = "visitas")
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreVisitante;

    @Column(length = 30)
    private String identificacion;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDestinatario tipoDestinatario = TipoDestinatario.DOCENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_visitada_id")
    private Usuario personaVisitada;

    @Column(nullable = false)
    private Boolean tieneCita = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVisita estado = EstadoVisita.PENDIENTE;

    private LocalDateTime fechaHoraIngreso;

    private LocalDateTime fechaHoraSalida;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_id")
    private Institucion institucion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public enum TipoDestinatario {
        DIRECTORA, DOCENTE, ADMINISTRATIVO
    }

    public enum EstadoVisita {
        PENDIENTE, AUTORIZADA, DENEGADA, FINALIZADA
    }

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoVisita.PENDIENTE;
        }
    }

    public Visita() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreVisitante() {
        return nombreVisitante;
    }

    public void setNombreVisitante(String nombreVisitante) {
        this.nombreVisitante = nombreVisitante;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public TipoDestinatario getTipoDestinatario() {
        return tipoDestinatario;
    }

    public void setTipoDestinatario(TipoDestinatario tipoDestinatario) {
        this.tipoDestinatario = tipoDestinatario;
    }

    public Usuario getPersonaVisitada() {
        return personaVisitada;
    }

    public void setPersonaVisitada(Usuario personaVisitada) {
        this.personaVisitada = personaVisitada;
    }

    public Boolean getTieneCita() {
        return tieneCita;
    }

    public void setTieneCita(Boolean tieneCita) {
        this.tieneCita = tieneCita;
    }

    public EstadoVisita getEstado() {
        return estado;
    }

    public void setEstado(EstadoVisita estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaHoraIngreso() {
        return fechaHoraIngreso;
    }

    public void setFechaHoraIngreso(LocalDateTime fechaHoraIngreso) {
        this.fechaHoraIngreso = fechaHoraIngreso;
    }

    public LocalDateTime getFechaHoraSalida() {
        return fechaHoraSalida;
    }

    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) {
        this.fechaHoraSalida = fechaHoraSalida;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Institucion getInstitucion() {
        return institucion;
    }

    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
