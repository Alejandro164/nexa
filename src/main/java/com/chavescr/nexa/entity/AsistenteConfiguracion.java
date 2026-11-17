package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Conexión del Chat Asistente con la API de xAI (Grok). Es una sola para todo el sistema: la administra
 * System Config y la llave se guarda cifrada.
 */
@Entity
@Table(name = "asistente_configuracion")
public class AsistenteConfiguracion {

    public static final String MODELO_PREDETERMINADO = "grok-4.7";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_cifrada", columnDefinition = "TEXT")
    private String apiKeyCifrada;

    @ColumnDefault("'" + MODELO_PREDETERMINADO + "'")
    @Column(nullable = false, length = 60)
    private String modelo = MODELO_PREDETERMINADO;

    @ColumnDefault("false")
    @Column(nullable = false)
    private Boolean activo = false;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultima_prueba_exitosa")
    private LocalDateTime ultimaPruebaExitosa;

    public boolean tieneApiKey() {
        return apiKeyCifrada != null && !apiKeyCifrada.isBlank();
    }

    /** Activo y con llave: el botón del chat aparece para los usuarios. */
    public boolean isDisponible() {
        return Boolean.TRUE.equals(activo) && tieneApiKey();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiKeyCifrada() {
        return apiKeyCifrada;
    }

    public void setApiKeyCifrada(String apiKeyCifrada) {
        this.apiKeyCifrada = apiKeyCifrada;
    }

    public String getModelo() {
        return modelo == null || modelo.isBlank() ? MODELO_PREDETERMINADO : modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
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
