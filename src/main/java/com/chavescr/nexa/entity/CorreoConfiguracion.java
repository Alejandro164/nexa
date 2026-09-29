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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Servidor SMTP propio de una dirección para el envío de notificaciones por correo. */
@Entity
@Table(name = "correo_configuraciones")
public class CorreoConfiguracion {

    /** Cifrado de la conexión con el servidor SMTP. */
    public enum Seguridad {
        // Sin cifrado (solo para servidores internos o de pruebas, ej. Mailpit)
        NINGUNA,
        // Conexión en claro que se eleva a TLS (típico en el puerto 587)
        STARTTLS,
        // TLS desde el inicio (típico en el puerto 465)
        SSL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false, unique = true)
    private Direccion direccion;

    @Column(length = 150)
    private String host;

    private Integer puerto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Seguridad seguridad = Seguridad.STARTTLS;

    @Column(length = 150)
    private String usuario;

    @Column(name = "password_cifrado", columnDefinition = "TEXT")
    private String passwordCifrado;

    @Column(name = "remitente_email", length = 150)
    private String remitenteEmail;

    @Column(name = "remitente_nombre", length = 150)
    private String remitenteNombre;

    @Column(nullable = false)
    private Boolean activo = false;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultima_prueba_exitosa")
    private LocalDateTime ultimaPruebaExitosa;

    public static CorreoConfiguracion predeterminada(Direccion direccion) {
        CorreoConfiguracion config = new CorreoConfiguracion();
        config.setDireccion(direccion);
        config.setPuerto(587);
        return config;
    }

    public boolean tienePassword() {
        return passwordCifrado != null && !passwordCifrado.isBlank();
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPuerto() {
        return puerto;
    }

    public void setPuerto(Integer puerto) {
        this.puerto = puerto;
    }

    public Seguridad getSeguridad() {
        return seguridad;
    }

    public void setSeguridad(Seguridad seguridad) {
        this.seguridad = seguridad;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPasswordCifrado() {
        return passwordCifrado;
    }

    public void setPasswordCifrado(String passwordCifrado) {
        this.passwordCifrado = passwordCifrado;
    }

    public String getRemitenteEmail() {
        return remitenteEmail;
    }

    public void setRemitenteEmail(String remitenteEmail) {
        this.remitenteEmail = remitenteEmail;
    }

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
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
