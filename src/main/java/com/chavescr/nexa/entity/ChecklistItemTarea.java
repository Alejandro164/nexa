package com.chavescr.nexa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Subtarea de la checklist de una tarea. */
@Entity
@Table(name = "checklist_items_tarea")
public class ChecklistItemTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id", nullable = false)
    private TareaPlanTrabajo tarea;

    @Column(nullable = false, length = 300)
    private String texto;

    @Column(nullable = false)
    private boolean completado = false;

    @Column(nullable = false)
    private int orden;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TareaPlanTrabajo getTarea() { return tarea; }
    public void setTarea(TareaPlanTrabajo tarea) { this.tarea = tarea; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
