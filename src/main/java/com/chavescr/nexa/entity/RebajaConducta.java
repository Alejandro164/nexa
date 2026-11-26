package com.chavescr.nexa.entity;

import org.hibernate.annotations.ColumnDefault;

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

/**
 * Una configuración por dirección. {@code reglas} guarda, en este orden, cuántos
 * registros forman un grupo ({@code cada}) y cuántos puntos baja la nota por cada
 * grupo completo ({@code puntos}). Cero puntos significa que ese registro no rebaja.
 * Las llamadas de atención rebajan la conducta. Las ausencias y tardías se calculan según
 * {@code modoAsistencia}: con el reglamento del MEP van al rubro Asistencia con la tabla del
 * artículo 37; con valores propios usan {@code reglas} y {@code destinoRebajaAsistencia} decide
 * si rebajan el rubro Asistencia o el total de la conducta.
 */
@Entity
@Table(name = "rebajas_conducta")
public class RebajaConducta {

    public static final int MAX_REGLAS = 500;

    /** Por defecto cada tardía injustificada es media ausencia (dos tardías forman una ausencia). */
    public static final int TARDIAS_POR_AUSENCIA_PREDETERMINADO = 2;

    public static final String REGLAS_PREDETERMINADAS = reglasPredeterminadas();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false, unique = true)
    private Direccion direccion;

    @Column(nullable = false, length = MAX_REGLAS)
    private String reglas = REGLAS_PREDETERMINADAS;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PROFESOR_GUIA'")
    @Column(nullable = false, length = 30)
    private QuienRegistraPuntosBoleta quienRegistraPuntosBoleta = QuienRegistraPuntosBoleta.PROFESOR_GUIA;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COMPONENTE'")
    @Column(nullable = false, length = 30)
    private DestinoRebajaAsistencia destinoRebajaAsistencia = DestinoRebajaAsistencia.COMPONENTE;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'REGLAMENTO_MEP'")
    @Column(nullable = false, length = 20)
    private ModoAsistencia modoAsistencia = ModoAsistencia.REGLAMENTO_MEP;

    /** Tardías injustificadas que forman una ausencia: 2 = media ausencia cada una, 1 = una ausencia. */
    @ColumnDefault("2")
    @Column(nullable = false)
    private Integer tardiasPorAusencia = TARDIAS_POR_AUSENCIA_PREDETERMINADO;

    /**
     * Ausencias justificadas que forman una injustificada con el reglamento del MEP. 0 = no cuentan (lo que dice el
     * reglamento); N = cada N justificadas completas suman una injustificada.
     */
    @ColumnDefault("0")
    @Column(name = "justificadas_por_ausencia", nullable = false)
    private Integer justificadasPorAusencia = 0;

    public static RebajaConducta predeterminada() {
        return new RebajaConducta();
    }

    public static String reglasPredeterminadas() {
        StringBuilder out = new StringBuilder("[");
        TipoRebaja[] tipos = TipoRebaja.values();
        for (int i = 0; i < tipos.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            TipoRebaja tipo = tipos[i];
            out.append("{\"id\":\"").append(tipo.id())
                    .append("\",\"cada\":").append(tipo.cada())
                    .append(",\"puntos\":").append(tipo.puntos())
                    .append('}');
        }
        return out.append(']').toString();
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

    public String getReglas() {
        return reglas;
    }

    public void setReglas(String reglas) {
        this.reglas = reglas;
    }

    public QuienRegistraPuntosBoleta getQuienRegistraPuntosBoleta() {
        return quienRegistraPuntosBoleta;
    }

    public void setQuienRegistraPuntosBoleta(QuienRegistraPuntosBoleta quienRegistraPuntosBoleta) {
        this.quienRegistraPuntosBoleta = quienRegistraPuntosBoleta;
    }

    public ModoAsistencia getModoAsistencia() {
        return modoAsistencia == null ? ModoAsistencia.REGLAMENTO_MEP : modoAsistencia;
    }

    public void setModoAsistencia(ModoAsistencia modoAsistencia) {
        this.modoAsistencia = modoAsistencia;
    }

    public Integer getTardiasPorAusencia() {
        return tardiasPorAusencia == null ? TARDIAS_POR_AUSENCIA_PREDETERMINADO : tardiasPorAusencia;
    }

    public void setTardiasPorAusencia(Integer tardiasPorAusencia) {
        this.tardiasPorAusencia = tardiasPorAusencia;
    }

    public DestinoRebajaAsistencia getDestinoRebajaAsistencia() {
        return destinoRebajaAsistencia;
    }

    public void setDestinoRebajaAsistencia(DestinoRebajaAsistencia destinoRebajaAsistencia) {
        this.destinoRebajaAsistencia = destinoRebajaAsistencia;
    }

    public Integer getJustificadasPorAusencia() {
        return justificadasPorAusencia == null ? 0 : justificadasPorAusencia;
    }

    public void setJustificadasPorAusencia(Integer justificadasPorAusencia) {
        this.justificadasPorAusencia = justificadasPorAusencia;
    }
}
