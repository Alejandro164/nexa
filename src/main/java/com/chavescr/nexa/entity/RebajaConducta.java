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

/**
 * Una configuración por dirección. {@code reglas} guarda, en este orden, cuántos
 * registros forman un grupo ({@code cada}) y cuántos puntos baja la nota por cada
 * grupo completo ({@code puntos}). Cero puntos significa que ese registro no rebaja.
 */
@Entity
@Table(name = "rebajas_conducta")
public class RebajaConducta {

    public static final int MAX_REGLAS = 500;

    public static final String REGLAS_PREDETERMINADAS = reglasPredeterminadas();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direccion_id", nullable = false, unique = true)
    private Direccion direccion;

    @Column(nullable = false, length = MAX_REGLAS)
    private String reglas = REGLAS_PREDETERMINADAS;

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
}
