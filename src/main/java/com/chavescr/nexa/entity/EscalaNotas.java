package com.chavescr.nexa.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Una escala por dirección y ciclo educativo: los estudiantes de una sección usan la de su ciclo. {@code tramos} va de la nota más alta a la más baja:
 * {@code [{"nombre":"Excelente","desde":90}, ...]}. El tope de cada tramo es el
 * inicio del anterior; el primero llega hasta {@code notaMaxima}. Se aprueba
 * desde {@code notaAprobacion}.
 */
@Entity
@Table(name = "escalas_notas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_escala_notas_direccion_ciclo", columnNames = { "direccion_id", "ciclo" })
})
public class EscalaNotas {

    public static final int NOTA_MINIMA_PREDETERMINADA = 0;
    public static final int NOTA_MAXIMA_PREDETERMINADA = 100;
    public static final int DECIMALES_PREDETERMINADOS = 1;
    public static final int NOTA_APROBACION_PREDETERMINADA = 70;
    public static final String TRAMOS_PREDETERMINADOS =
            "[{\"nombre\":\"Excelente\",\"desde\":90},{\"nombre\":\"Bueno\",\"desde\":80},"
                    + "{\"nombre\":\"Regular\",\"desde\":70},{\"nombre\":\"Aplazado\",\"desde\":0}]";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false)
    private Direccion direccion;

    /** Nullable en el esquema solo para la migración desde la escala única por dirección. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CicloEducativo ciclo;

    @Column(nullable = false)
    private Integer notaMinima = NOTA_MINIMA_PREDETERMINADA;

    @Column(nullable = false)
    private Integer notaMaxima = NOTA_MAXIMA_PREDETERMINADA;

    @Column(nullable = false)
    private Integer decimales = DECIMALES_PREDETERMINADOS;

    @Column(nullable = false)
    private Integer notaAprobacion = NOTA_APROBACION_PREDETERMINADA;

    @Column(nullable = false, length = 800)
    private String tramos = TRAMOS_PREDETERMINADOS;

    public static EscalaNotas predeterminada() {
        return new EscalaNotas();
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

    public CicloEducativo getCiclo() {
        return ciclo;
    }

    public void setCiclo(CicloEducativo ciclo) {
        this.ciclo = ciclo;
    }

    public Integer getNotaMinima() {
        return notaMinima;
    }

    public void setNotaMinima(Integer notaMinima) {
        this.notaMinima = notaMinima;
    }

    public Integer getNotaMaxima() {
        return notaMaxima;
    }

    public void setNotaMaxima(Integer notaMaxima) {
        this.notaMaxima = notaMaxima;
    }

    public Integer getDecimales() {
        return decimales;
    }

    public void setDecimales(Integer decimales) {
        this.decimales = decimales;
    }

    public Integer getNotaAprobacion() {
        return notaAprobacion;
    }

    public void setNotaAprobacion(Integer notaAprobacion) {
        this.notaAprobacion = notaAprobacion;
    }

    public String getTramos() {
        return tramos;
    }

    public void setTramos(String tramos) {
        this.tramos = tramos;
    }
}
