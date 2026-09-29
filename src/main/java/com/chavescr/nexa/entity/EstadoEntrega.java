package com.chavescr.nexa.entity;

public enum EstadoEntrega {
    // Asignada: aún no entregada (o el estudiante anuló la entrega); conserva lo que ya adjuntó
    ASIGNADA,
    ENTREGADA,
    // El docente la revisó (y calificó, si la tarea es calificable)
    DEVUELTA
}
