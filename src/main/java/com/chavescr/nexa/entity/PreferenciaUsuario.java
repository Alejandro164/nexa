package com.chavescr.nexa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Preferencias personales de cuenta (no confundir con la Configuración Institucional). */
@Entity
@Table(name = "preferencias_usuario")
public class PreferenciaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean notifEmail = true;

    @Column(nullable = false)
    private Boolean notifPush = true;

    @Column(nullable = false)
    private Boolean notifRecordatorios = true;

    @Column(nullable = false)
    private Boolean notifResumenSemanal = false;

    @Column(nullable = false, length = 20)
    private String idioma = "es-CR";

    @Column(nullable = false, length = 40)
    private String zonaHoraria = "America/Costa_Rica";

    @Column(nullable = false, length = 20)
    private String tema = "claro";

    public PreferenciaUsuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Boolean getNotifEmail() {
        return notifEmail;
    }

    public void setNotifEmail(Boolean notifEmail) {
        this.notifEmail = notifEmail;
    }

    public Boolean getNotifPush() {
        return notifPush;
    }

    public void setNotifPush(Boolean notifPush) {
        this.notifPush = notifPush;
    }

    public Boolean getNotifRecordatorios() {
        return notifRecordatorios;
    }

    public void setNotifRecordatorios(Boolean notifRecordatorios) {
        this.notifRecordatorios = notifRecordatorios;
    }

    public Boolean getNotifResumenSemanal() {
        return notifResumenSemanal;
    }

    public void setNotifResumenSemanal(Boolean notifResumenSemanal) {
        this.notifResumenSemanal = notifResumenSemanal;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public void setZonaHoraria(String zonaHoraria) {
        this.zonaHoraria = zonaHoraria;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }
}
