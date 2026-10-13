package com.chavescr.nexa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Oficio administrativo (correspondencia oficial saliente de la dirección). El documento firmado es un PDF subido por el usuario, no generado por el sistema. */
@Entity
@Table(name = "oficios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oficio_direccion_numero", columnNames = {"direccion_id", "numero"})
})
public class Oficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    @Column(nullable = false, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redactado_por_id")
    private Usuario redactadoPor;

    @Column(nullable = false, length = 300)
    private String asunto;

    /**
     * Dirección externa a la que se envía el documento por correo al emitir. Es opcional: un oficio puede
     * dirigirse solo a personas o grupos internos ({@link #destinatarios}) y verse en Comunicados.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_direccion_id")
    private Direccion destinatarioDireccion;

    @Column(name = "numero_circular", length = 30)
    private String numeroCircular;

    @Column(nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(nullable = false)
    private LocalDate fecha;

    /** El documento vive en Nube Nexa (carpeta "Oficios" de la dirección) — no se duplica su ruta/nombre aquí. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nube_nodo_id")
    private NubeNodo nubeNodo;

    /** Momento exacto de la emisión; {@link #fecha} solo guarda el día. */
    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    /** Personas y grupos internos que ven el oficio en Comunicados una vez emitido. */
    @OneToMany(mappedBy = "oficio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OficioDestinatario> destinatarios = new ArrayList<>();

    @PrePersist
    void alPersistir() {
        if (fecha == null) {
            fecha = LocalDate.now();
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

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Usuario getRedactadoPor() {
        return redactadoPor;
    }

    public void setRedactadoPor(Usuario redactadoPor) {
        this.redactadoPor = redactadoPor;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public Direccion getDestinatarioDireccion() {
        return destinatarioDireccion;
    }

    public void setDestinatarioDireccion(Direccion destinatarioDireccion) {
        this.destinatarioDireccion = destinatarioDireccion;
    }

    public String getNumeroCircular() {
        return numeroCircular;
    }

    public void setNumeroCircular(String numeroCircular) {
        this.numeroCircular = numeroCircular;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public List<OficioDestinatario> getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(List<OficioDestinatario> destinatarios) {
        this.destinatarios = destinatarios;
    }

    public NubeNodo getNubeNodo() {
        return nubeNodo;
    }

    public void setNubeNodo(NubeNodo nubeNodo) {
        this.nubeNodo = nubeNodo;
    }
}
