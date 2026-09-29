package com.chavescr.nexa.dto;

import java.util.List;

import com.chavescr.nexa.entity.ClassroomClase;
import com.chavescr.nexa.entity.ClassroomPublicacion;

/** Tarjeta de una clase en el listado principal del Classroom. */
public class TarjetaClase {

    private final ClassroomClase clase;
    private final RolClase rol;
    private final String docentes;
    private final int totalEstudiantes;
    // Estudiante: próximas tareas que aún no entregó
    private final List<ClassroomPublicacion> proximas;
    // Docente: entregas recibidas pendientes de revisar
    private final long porRevisar;

    public TarjetaClase(ClassroomClase clase, RolClase rol, String docentes, int totalEstudiantes,
            List<ClassroomPublicacion> proximas, long porRevisar) {
        this.clase = clase;
        this.rol = rol;
        this.docentes = docentes;
        this.totalEstudiantes = totalEstudiantes;
        this.proximas = proximas;
        this.porRevisar = porRevisar;
    }

    public ClassroomClase getClase() {
        return clase;
    }

    public RolClase getRol() {
        return rol;
    }

    public String getDocentes() {
        return docentes;
    }

    public int getTotalEstudiantes() {
        return totalEstudiantes;
    }

    public List<ClassroomPublicacion> getProximas() {
        return proximas;
    }

    public long getPorRevisar() {
        return porRevisar;
    }
}
