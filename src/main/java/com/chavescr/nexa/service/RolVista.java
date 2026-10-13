package com.chavescr.nexa.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;

/** Cómo se presenta el rol de una persona (etiqueta, color de la insignia e iniciales) en la mensajería y en Comunicados. */
final class RolVista {

    private static final Locale ES = Locale.forLanguageTag("es-CR");
    private static final List<String> ORDEN_ROLES = List.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DOCENTE",
            "ROLE_PADRE", "ROLE_ESTUDIANTE");

    private RolVista() {
    }

    /** El rol que se muestra cuando la persona tiene varios (el de mayor responsabilidad). */
    static String principal(Usuario usuario) {
        Set<String> nombres = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
        return ORDEN_ROLES.stream().filter(nombres::contains).findFirst().orElse("");
    }

    static String etiqueta(String rol) {
        return switch (rol) {
            case "ROLE_ADMIN" -> "Administración";
            case "ROLE_DIRECTOR" -> "Dirección";
            case "ROLE_DOCENTE" -> "Docente";
            case "ROLE_PADRE" -> "Familia";
            case "ROLE_ESTUDIANTE" -> "Estudiante";
            default -> "";
        };
    }

    /** Clase CSS de la insignia: institucion, docente, familia o estudiante (vacía si no hay rol conocido). */
    static String clase(String rol) {
        return switch (rol) {
            case "ROLE_ADMIN", "ROLE_DIRECTOR" -> "institucion";
            case "ROLE_DOCENTE" -> "docente";
            case "ROLE_PADRE" -> "familia";
            case "ROLE_ESTUDIANTE" -> "estudiante";
            default -> "";
        };
    }

    static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.strip().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (primera + segunda).toUpperCase(ES);
    }
}
