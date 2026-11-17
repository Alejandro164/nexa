package com.chavescr.nexa.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Quién usa el Chat Asistente: el asistente solo ve lo que esta persona puede ver en la aplicación.
 * Se arma en el controlador con la sesión y los roles reales (no con el "Modo" del menú).
 */
public record ContextoAsistente(Long direccionId, String direccionNombre, Long usuarioId, String nombre,
        Set<String> roles) {

    /** Administración, dirección y System Config ven toda la dirección. */
    public boolean esGestion() {
        return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DIRECTOR") || roles.contains("ROLE_SYSTEM_CONFIG");
    }

    public boolean esDocente() {
        return roles.contains("ROLE_DOCENTE");
    }

    public boolean esPadre() {
        return roles.contains("ROLE_PADRE");
    }

    public boolean esEstudiante() {
        return roles.contains("ROLE_ESTUDIANTE");
    }

    /** Roles en palabras, para el contexto que recibe el modelo. */
    public String rolesLegibles() {
        List<String> nombres = new ArrayList<>();
        if (roles.contains("ROLE_SYSTEM_CONFIG")) {
            nombres.add("System Config");
        }
        if (roles.contains("ROLE_ADMIN")) {
            nombres.add("administrador");
        }
        if (roles.contains("ROLE_DIRECTOR")) {
            nombres.add("director");
        }
        if (esDocente()) {
            nombres.add("docente");
        }
        if (esPadre()) {
            nombres.add("padre o madre de familia");
        }
        if (esEstudiante()) {
            nombres.add("estudiante");
        }
        return nombres.isEmpty() ? "sin rol académico" : String.join(", ", nombres);
    }
}
