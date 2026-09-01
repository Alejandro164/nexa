package com.chavescr.nexa.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Oficio administrativo (correspondencia oficial saliente de la institución). El documento firmado es un PDF subido por el usuario, no generado por el sistema. */
@Entity
@Table(name = "oficios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oficio_institucion_numero", columnNames = {"institucion_id", "numero"})
})
public class Oficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @Column(nullable = false, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redactado_por_id")
    private Usuario redactadoPor;

    @Column(nullable = false, length = 300)
    private String asunto;

    @Column(nullable = false, length = 200)
    private String destinatario;

    @Column(name = "numero_circular", length = 30)
    private String numeroCircular;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "ruta_archivo", length = 300)
    private String rutaArchivo;

    @Column(name = "nombre_archivo_original", length = 200)
    private String nombreArchivoOriginal;

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

    public Institucion getInstitucion() {
        return institucion;
    }

    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
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

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
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

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getNombreArchivoOriginal() {
        return nombreArchivoOriginal;
    }

    public void setNombreArchivoOriginal(String nombreArchivoOriginal) {
        this.nombreArchivoOriginal = nombreArchivoOriginal;
    }
}
