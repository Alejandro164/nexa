package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.EntregaAcademica;
import com.chavescr.nexa.entity.EntregaAcademicaDocente;
import com.chavescr.nexa.entity.EntregaAcademicaDocente.EstadoPresentacion;
import com.chavescr.nexa.entity.Usuario;

/**
 * Una fila de las tablas de Pendientes/Realizadas y del seguimiento: una entrega y un docente, con su
 * respuesta si ya la empezó (si no, {@code presentacion} es null y el estado es "Sin iniciar").
 */
public class FilaEntregaAcademica {

    private final EntregaAcademica entrega;
    private final Usuario docente;
    private final EntregaAcademicaDocente presentacion;

    public FilaEntregaAcademica(EntregaAcademica entrega, Usuario docente, EntregaAcademicaDocente presentacion) {
        this.entrega = entrega;
        this.docente = docente;
        this.presentacion = presentacion;
    }

    public EntregaAcademica getEntrega() { return entrega; }
    public Usuario getDocente() { return docente; }
    public EntregaAcademicaDocente getPresentacion() { return presentacion; }

    public EstadoPresentacion getEstado() { return presentacion != null ? presentacion.getEstado() : null; }

    public String getEstadoEtiqueta() { return presentacion != null ? presentacion.getEstado().getEtiqueta() : "Sin iniciar"; }

    public String getEstadoCss() { return presentacion != null ? presentacion.getEstado().getCss() : "sin-iniciar"; }

    /** Texto del botón de la fila para el docente: Iniciar, Continuar, Corregir o Ver. */
    public String getAccionDocente() {
        if (presentacion == null) return "Iniciar";
        return switch (presentacion.getEstado()) {
            case BORRADOR -> "Continuar";
            case DEVUELTA -> "Corregir";
            default -> "Ver";
        };
    }

    /** Texto del botón de la fila para la dirección: Revisar si espera revisión, si no Ver. */
    public String getAccionDireccion() {
        return presentacion != null && presentacion.isPorRevisar() ? "Revisar" : "Ver";
    }
}
