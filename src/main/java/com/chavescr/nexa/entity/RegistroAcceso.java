package com.chavescr.nexa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Bitácora de accesos (login/logout) de todos los usuarios, para el Centro de Seguridad. */
@Entity
@Table(name = "registros_acceso")
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // Email real en login exitoso/logout; en un login fallido, exactamente lo que se escribió en el
    // formulario (puede no corresponder a ninguna cuenta real — se registra igual, para trazabilidad).
    @Column(name = "usuario_identificador", length = 150)
    private String usuarioIdentificador;

    @Column(length = 45)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccionAcceso accion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoAcceso resultado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getUsuarioIdentificador() {
        return usuarioIdentificador;
    }

    public void setUsuarioIdentificador(String usuarioIdentificador) {
        this.usuarioIdentificador = usuarioIdentificador;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public AccionAcceso getAccion() {
        return accion;
    }

    public void setAccion(AccionAcceso accion) {
        this.accion = accion;
    }

    public ResultadoAcceso getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAcceso resultado) {
        this.resultado = resultado;
    }
}
