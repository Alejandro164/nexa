package com.chavescr.nexa.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum DiaLaboral {
    LUNES("Lunes", "Lun"),
    MARTES("Martes", "Mar"),
    MIERCOLES("Miércoles", "Mié"),
    JUEVES("Jueves", "Jue"),
    VIERNES("Viernes", "Vie"),
    SABADO("Sábado", "Sáb");

    public static final List<String> CATALOGO = Arrays.stream(values()).map(Enum::name).toList();
    public static final List<String> PREDETERMINADOS = List.of(
            LUNES.name(), MARTES.name(), MIERCOLES.name(), JUEVES.name(), VIERNES.name());

    private final String etiqueta;
    private final String etiquetaCorta;

    DiaLaboral(String etiqueta, String etiquetaCorta) {
        this.etiqueta = etiqueta;
        this.etiquetaCorta = etiquetaCorta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String etiquetaCorta() {
        return etiquetaCorta;
    }

    public static String etiqueta(String dia) {
        return parsearCodigo(dia).map(DiaLaboral::etiqueta).orElse(dia);
    }

    public static String etiquetaCorta(String dia) {
        return parsearCodigo(dia).map(DiaLaboral::etiquetaCorta).orElse(dia);
    }

    public static List<String> parsear(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(dia -> !dia.isEmpty())
                .toList();
    }

    public static String serializar(List<String> dias) {
        List<String> ordenados = new ArrayList<>();
        for (String dia : CATALOGO) {
            if (dias != null && dias.contains(dia)) {
                ordenados.add(dia);
            }
        }
        return String.join(",", ordenados);
    }

    private static Optional<DiaLaboral> parsearCodigo(String dia) {
        if (dia == null || dia.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(DiaLaboral.valueOf(dia.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
