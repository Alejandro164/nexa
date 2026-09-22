package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Credenciales de WhatsApp Business Cloud API (Meta) para el envío de notificaciones de una institución. */
@Entity
@Table(name = "whatsapp_configuraciones")
public class WhatsAppConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false, unique = true)
    private Institucion institucion;

    @Column(name = "phone_number_id", length = 50)
    private String phoneNumberId;

    @Column(name = "business_account_id", length = 50)
    private String businessAccountId;

    @Column(name = "access_token_cifrado", columnDefinition = "TEXT")
    private String accessTokenCifrado;

    @Column(name = "numero_mostrar", length = 30)
    private String numeroMostrar;

    @Column(nullable = false)
    private Boolean activo = false;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultima_prueba_exitosa")
    private LocalDateTime ultimaPruebaExitosa;

    public static WhatsAppConfiguracion predeterminada(Institucion institucion) {
        WhatsAppConfiguracion config = new WhatsAppConfiguracion();
        config.setInstitucion(institucion);
        return config;
    }

    public boolean tieneToken() {
        return accessTokenCifrado != null && !accessTokenCifrado.isBlank();
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

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(String businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public String getAccessTokenCifrado() {
        return accessTokenCifrado;
    }

    public void setAccessTokenCifrado(String accessTokenCifrado) {
        this.accessTokenCifrado = accessTokenCifrado;
    }

    public String getNumeroMostrar() {
        return numeroMostrar;
    }

    public void setNumeroMostrar(String numeroMostrar) {
        this.numeroMostrar = numeroMostrar;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public LocalDateTime getUltimaPruebaExitosa() {
        return ultimaPruebaExitosa;
    }

    public void setUltimaPruebaExitosa(LocalDateTime ultimaPruebaExitosa) {
        this.ultimaPruebaExitosa = ultimaPruebaExitosa;
    }
}
