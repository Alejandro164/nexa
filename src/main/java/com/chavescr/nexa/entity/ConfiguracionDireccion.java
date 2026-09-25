package com.chavescr.nexa.entity;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.chavescr.nexa.entity.Jornada.BloqueJornada;
import com.chavescr.nexa.entity.Jornada.FranjaHoraria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuraciones_direccion")
public class ConfiguracionDireccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false, unique = true)
    private Direccion direccion;

    @Column(nullable = false)
    private Integer cantidadLecciones = Jornada.CANTIDAD_LECCIONES_PREDETERMINADA;

    @Column(nullable = false)
    private LocalTime inicioJornada = Jornada.INICIO_JORNADA_PREDETERMINADO;

    @Column(nullable = false)
    private Integer minutosLeccion = Jornada.MINUTOS_LECCION_PREDETERMINADOS;

    @Column(nullable = false)
    private Integer minutosRecreo = Jornada.MINUTOS_RECREO_PREDETERMINADOS;

    @Column(name = "duraciones_recreos", length = 80)
    private String duracionesRecreos = Jornada.serializarDuraciones(Jornada.DURACIONES_RECREOS_PREDETERMINADAS);

    @Column(nullable = false)
    private Integer leccionesPorBloque = Jornada.LECCIONES_POR_BLOQUE_PREDETERMINADAS;

    @Column(name = "leccion_almuerzo")
    private Integer leccionAlmuerzo = Jornada.LECCION_ALMUERZO_PREDETERMINADA;

    @Column(name = "minutos_almuerzo")
    private Integer minutosAlmuerzo = Jornada.MINUTOS_ALMUERZO_PREDETERMINADOS;

    @Column(name = "bloques_jornada", length = 255)
    private String bloquesJornada;

    @Column(name = "dias_laborales", nullable = false, length = 80)
    private String diasLaborales = DiaLaboral.serializar(DiaLaboral.PREDETERMINADOS);

    public static ConfiguracionDireccion predeterminada(Direccion direccion) {
        ConfiguracionDireccion config = new ConfiguracionDireccion();
        config.setDireccion(direccion);
        return config;
    }

    public Jornada jornada() {
        return Jornada.de(inicioJornada, minutosLeccion, bloquesJornada, cantidadLecciones, leccionesPorBloque,
                leccionAlmuerzo, minutosAlmuerzo, minutosRecreo, listaDuracionesRecreos());
    }

    public void aplicarBloques(List<BloqueJornada> bloques) {
        Jornada.AplicacionBloques aplicada = Jornada.aplicar(bloques);
        this.bloquesJornada = Jornada.serializarBloques(aplicada.bloques());
        this.cantidadLecciones = aplicada.cantidadLecciones();
        this.leccionesPorBloque = aplicada.leccionesPorBloque();
        this.leccionAlmuerzo = aplicada.leccionAlmuerzo();
        this.minutosAlmuerzo = aplicada.minutosAlmuerzo();
        this.minutosRecreo = aplicada.minutosRecreo();
        setListaDuracionesRecreos(aplicada.duracionesRecreos());
    }

    public List<String> getDias() {
        return DiaLaboral.parsear(diasLaborales);
    }

    public void setDias(List<String> dias) {
        this.diasLaborales = DiaLaboral.serializar(dias);
    }

    public List<Integer> getLecciones() {
        return jornada().lecciones();
    }

    public List<BloqueJornada> bloques() {
        return jornada().bloques();
    }

    public String serializarBloques() {
        return jornada().serializarBloques();
    }

    public List<Integer> listaDuracionesRecreos() {
        return Jornada.parsearDuraciones(duracionesRecreos);
    }

    public void setListaDuracionesRecreos(List<Integer> minutos) {
        this.duracionesRecreos = Jornada.serializarDuraciones(minutos);
    }

    public LocalTime horaInicioLeccion(int numeroLeccion) {
        return jornada().horaInicioLeccion(numeroLeccion);
    }

    public LocalTime horaFinLeccion(int numeroLeccion) {
        return jornada().horaFinLeccion(numeroLeccion);
    }

    public boolean hayAlmuerzoDespues(int numeroLeccion) {
        return jornada().hayAlmuerzoDespues(numeroLeccion);
    }

    public boolean hayRecreoDespues(int numeroLeccion) {
        return jornada().hayRecreoDespues(numeroLeccion);
    }

    public int minutosRecreoDespues(int numeroLeccion) {
        return jornada().minutosRecreoDespues(numeroLeccion);
    }

    public String etiquetaRecreos() {
        return jornada().etiquetaRecreos();
    }

    public Map<Integer, FranjaHoraria> franjas() {
        return jornada().franjas();
    }

    public Map<Integer, FranjaHoraria> recreos() {
        return jornada().recreos();
    }

    public Map<Integer, FranjaHoraria> almuerzos() {
        return jornada().almuerzos();
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

    public Integer getCantidadLecciones() {
        return cantidadLecciones;
    }

    public void setCantidadLecciones(Integer cantidadLecciones) {
        this.cantidadLecciones = cantidadLecciones;
    }

    public LocalTime getInicioJornada() {
        return inicioJornada;
    }

    public void setInicioJornada(LocalTime inicioJornada) {
        this.inicioJornada = inicioJornada;
    }

    public Integer getMinutosLeccion() {
        return minutosLeccion;
    }

    public void setMinutosLeccion(Integer minutosLeccion) {
        this.minutosLeccion = minutosLeccion;
    }

    public Integer getMinutosRecreo() {
        return minutosRecreo;
    }

    public void setMinutosRecreo(Integer minutosRecreo) {
        this.minutosRecreo = minutosRecreo;
    }

    public String getDuracionesRecreos() {
        return duracionesRecreos;
    }

    public void setDuracionesRecreos(String duracionesRecreos) {
        this.duracionesRecreos = duracionesRecreos;
    }

    public Integer getLeccionesPorBloque() {
        return leccionesPorBloque;
    }

    public void setLeccionesPorBloque(Integer leccionesPorBloque) {
        this.leccionesPorBloque = leccionesPorBloque;
    }

    public Integer getLeccionAlmuerzo() {
        return leccionAlmuerzo;
    }

    public void setLeccionAlmuerzo(Integer leccionAlmuerzo) {
        this.leccionAlmuerzo = leccionAlmuerzo;
    }

    public Integer getMinutosAlmuerzo() {
        return minutosAlmuerzo;
    }

    public void setMinutosAlmuerzo(Integer minutosAlmuerzo) {
        this.minutosAlmuerzo = minutosAlmuerzo;
    }

    public String getBloquesJornada() {
        return bloquesJornada;
    }

    public void setBloquesJornada(String bloquesJornada) {
        this.bloquesJornada = bloquesJornada;
    }

    public String getDiasLaborales() {
        return diasLaborales;
    }

    public void setDiasLaborales(String diasLaborales) {
        this.diasLaborales = diasLaborales;
    }
}
