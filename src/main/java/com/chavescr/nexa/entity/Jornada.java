package com.chavescr.nexa.entity;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Jornada {

    public static final int CANTIDAD_LECCIONES_PREDETERMINADA = 12;
    public static final int MAX_LECCIONES = 16;
    public static final LocalTime INICIO_JORNADA_PREDETERMINADO = LocalTime.of(7, 0);
    public static final int MINUTOS_LECCION_PREDETERMINADOS = 40;
    public static final int MINUTOS_RECREO_PREDETERMINADOS = 10;
    public static final List<Integer> DURACIONES_RECREOS_PREDETERMINADAS = List.of(15, 20, 10);
    public static final int LECCIONES_POR_BLOQUE_PREDETERMINADAS = 2;
    public static final int LECCION_ALMUERZO_PREDETERMINADA = 6;
    public static final int MINUTOS_ALMUERZO_PREDETERMINADOS = 40;

    public enum TipoPausa {
        RECESO("receso", 60),
        ALMUERZO("almuerzo", 120),
        NINGUNA("ninguna", 0);

        private final String codigo;
        private final int maxMinutos;

        TipoPausa(String codigo, int maxMinutos) {
            this.codigo = codigo;
            this.maxMinutos = maxMinutos;
        }

        public String codigo() {
            return codigo;
        }

        public int limitarMinutos(int minutos) {
            if (this == NINGUNA) {
                return 0;
            }
            return Math.max(0, Math.min(maxMinutos, minutos));
        }

        public static TipoPausa de(String raw) {
            if (raw == null) {
                return NINGUNA;
            }
            return switch (raw.trim().toLowerCase()) {
                case "receso" -> RECESO;
                case "almuerzo" -> ALMUERZO;
                default -> NINGUNA;
            };
        }
    }

    public record BloqueJornada(int lecciones, TipoPausa pausa, int minutos) {
        public BloqueJornada {
            pausa = pausa == null ? TipoPausa.NINGUNA : pausa;
            minutos = pausa.limitarMinutos(minutos);
        }

        public BloqueJornada(int lecciones, String pausa, int minutos) {
            this(lecciones, TipoPausa.de(pausa), minutos);
        }

        public boolean esReceso() {
            return pausa == TipoPausa.RECESO && minutos > 0;
        }

        public boolean esAlmuerzo() {
            return pausa == TipoPausa.ALMUERZO && minutos > 0;
        }

        public String codigoPausa() {
            return pausa.codigo();
        }
    }

    public record FranjaHoraria(LocalTime inicio, LocalTime fin) {
        public LocalTime getInicio() {
            return inicio;
        }

        public LocalTime getFin() {
            return fin;
        }

        public int getMinutos() {
            return (int) Duration.between(inicio, fin).toMinutes();
        }
    }

    public record AplicacionBloques(List<BloqueJornada> bloques, int cantidadLecciones, int leccionesPorBloque,
            Integer leccionAlmuerzo, int minutosAlmuerzo, int minutosRecreo, List<Integer> duracionesRecreos) {
    }

    private final LocalTime inicio;
    private final int minutosLeccion;
    private final List<BloqueJornada> bloques;

    public Jornada(LocalTime inicio, Integer minutosLeccion, List<BloqueJornada> bloques) {
        this.inicio = Objects.requireNonNullElse(inicio, INICIO_JORNADA_PREDETERMINADO);
        this.minutosLeccion = Objects.requireNonNullElse(minutosLeccion, MINUTOS_LECCION_PREDETERMINADOS);
        this.bloques = bloques == null ? List.of() : List.copyOf(bloques);
    }

    public static Jornada de(LocalTime inicio, Integer minutosLeccion, String bloquesJornada,
            Integer cantidadLecciones, Integer leccionesPorBloque, Integer leccionAlmuerzo,
            Integer minutosAlmuerzo, Integer minutosRecreo, List<Integer> duracionesRecreos) {
        List<BloqueJornada> bloques = bloquesJornada != null && !bloquesJornada.isBlank()
                ? parsearBloques(bloquesJornada)
                : reconstruir(cantidadLecciones, leccionesPorBloque, leccionAlmuerzo, minutosAlmuerzo,
                        minutosRecreo, duracionesRecreos);
        return new Jornada(inicio, minutosLeccion, bloques);
    }

    public static String serializarBloques(List<BloqueJornada> bloques) {
        if (bloques == null || bloques.isEmpty()) {
            return "";
        }
        return bloques.stream()
                .map(bloque -> bloque.lecciones() + ":" + bloque.codigoPausa() + ":" + bloque.minutos())
                .collect(Collectors.joining("|"));
    }

    public static List<BloqueJornada> parsearBloques(String raw) {
        return parsearBloques(raw, false);
    }

    public static List<BloqueJornada> parsearBloques(String raw, boolean estricto) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<BloqueJornada> bloques = new ArrayList<>();
        for (String parte : raw.split("\\|")) {
            String recorte = parte.trim();
            if (recorte.isEmpty()) {
                continue;
            }
            String[] bits = recorte.split(":");
            if (bits.length < 3) {
                if (estricto) {
                    throw new IllegalArgumentException("Hay un bloque de jornada inválido");
                }
                continue;
            }
            int lecciones;
            int minutos;
            try {
                lecciones = Integer.parseInt(bits[0].trim());
                minutos = Integer.parseInt(bits[2].trim());
            } catch (NumberFormatException e) {
                if (estricto) {
                    throw new IllegalArgumentException("Hay un bloque de jornada inválido");
                }
                continue;
            }
            if (lecciones < 1) {
                if (estricto) {
                    throw new IllegalArgumentException("Cada bloque debe tener entre 1 y 16 lecciones");
                }
                continue;
            }
            if (lecciones > MAX_LECCIONES) {
                if (estricto) {
                    throw new IllegalArgumentException("Cada bloque debe tener entre 1 y 16 lecciones");
                }
                lecciones = MAX_LECCIONES;
            }
            bloques.add(new BloqueJornada(lecciones, bits[1], minutos));
        }
        return bloques;
    }

    public static String serializarDuraciones(List<Integer> minutos) {
        if (minutos == null || minutos.isEmpty()) {
            return "";
        }
        return minutos.stream()
                .map(valor -> String.valueOf(limitarMinutosRecreo(valor)))
                .collect(Collectors.joining(","));
    }

    public static List<Integer> parsearDuraciones(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Integer> valores = new ArrayList<>();
        for (String parte : raw.split(",")) {
            String recorte = parte.trim();
            if (recorte.isEmpty()) {
                continue;
            }
            try {
                valores.add(limitarMinutosRecreo(Integer.parseInt(recorte)));
            } catch (NumberFormatException e) {
                valores.add(MINUTOS_RECREO_PREDETERMINADOS);
            }
        }
        return valores;
    }

    public static int limitarMinutosRecreo(Integer minutos) {
        int valor = minutos == null ? MINUTOS_RECREO_PREDETERMINADOS : minutos;
        return Math.max(0, Math.min(60, valor));
    }

    public static List<BloqueJornada> normalizarBloques(List<BloqueJornada> bloques) {
        if (bloques == null || bloques.isEmpty()) {
            return List.of();
        }
        List<BloqueJornada> limpios = new ArrayList<>();
        for (int i = 0; i < bloques.size(); i++) {
            BloqueJornada bloque = bloques.get(i);
            boolean ultimo = i == bloques.size() - 1;
            TipoPausa pausa = ultimo ? TipoPausa.NINGUNA : bloque.pausa();
            int minutos = ultimo ? 0 : pausa.limitarMinutos(bloque.minutos());
            if (minutos <= 0) {
                pausa = TipoPausa.NINGUNA;
                minutos = 0;
            }
            int lecciones = Math.min(MAX_LECCIONES, Math.max(1, bloque.lecciones()));
            limpios.add(new BloqueJornada(lecciones, pausa, minutos));
        }
        return List.copyOf(limpios);
    }

    public static AplicacionBloques aplicar(List<BloqueJornada> bloques) {
        List<BloqueJornada> limpios = normalizarBloques(bloques);
        int total = 0;
        List<Integer> recreos = new ArrayList<>();
        Integer leccionAlmuerzoNueva = null;
        int minutosAlmuerzoNuevos = 0;
        Integer primerTamano = null;
        int respaldoRecreo = MINUTOS_RECREO_PREDETERMINADOS;
        for (int i = 0; i < limpios.size(); i++) {
            BloqueJornada bloque = limpios.get(i);
            total += bloque.lecciones();
            if (primerTamano == null) {
                primerTamano = bloque.lecciones();
            }
            boolean ultimo = i == limpios.size() - 1;
            if (ultimo) {
                continue;
            }
            if (bloque.esAlmuerzo()) {
                leccionAlmuerzoNueva = total;
                minutosAlmuerzoNuevos = bloque.minutos();
            } else if (bloque.esReceso()) {
                recreos.add(bloque.minutos());
                respaldoRecreo = bloque.minutos();
            }
        }
        return new AplicacionBloques(
                limpios,
                total,
                primerTamano == null ? LECCIONES_POR_BLOQUE_PREDETERMINADAS : primerTamano,
                leccionAlmuerzoNueva,
                minutosAlmuerzoNuevos,
                respaldoRecreo,
                List.copyOf(recreos));
    }

    public List<BloqueJornada> bloques() {
        return bloques;
    }

    public List<Integer> lecciones() {
        return IntStream.rangeClosed(1, cantidadLecciones()).boxed().toList();
    }

    public int cantidadLecciones() {
        int suma = 0;
        for (BloqueJornada bloque : bloques) {
            suma += bloque.lecciones();
        }
        return suma;
    }

    public LocalTime horaInicioLeccion(int numeroLeccion) {
        LocalTime hora = inicio;
        int[] pausas = minutosPausaPorLeccion();
        for (int numero = 1; numero < numeroLeccion; numero++) {
            int extra = numero > 0 && numero < pausas.length ? pausas[numero] : 0;
            hora = hora.plusMinutes(minutosLeccion + extra);
        }
        return hora;
    }

    public LocalTime horaFinLeccion(int numeroLeccion) {
        return horaInicioLeccion(numeroLeccion).plusMinutes(minutosLeccion);
    }

    public boolean hayAlmuerzoDespues(int numeroLeccion) {
        BloqueJornada bloque = pausaTras(numeroLeccion);
        return bloque != null && bloque.esAlmuerzo();
    }

    public boolean hayRecreoDespues(int numeroLeccion) {
        return minutosRecreoDespues(numeroLeccion) > 0;
    }

    public int minutosRecreoDespues(int numeroLeccion) {
        BloqueJornada bloque = pausaTras(numeroLeccion);
        return bloque != null && bloque.esReceso() ? bloque.minutos() : 0;
    }

    public Map<Integer, FranjaHoraria> franjas() {
        Map<Integer, FranjaHoraria> franjas = new LinkedHashMap<>();
        for (Integer numero : lecciones()) {
            LocalTime inicioLeccion = horaInicioLeccion(numero);
            franjas.put(numero, new FranjaHoraria(inicioLeccion, inicioLeccion.plusMinutes(minutosLeccion)));
        }
        return franjas;
    }

    public Map<Integer, FranjaHoraria> recreos() {
        Map<Integer, FranjaHoraria> recreos = new LinkedHashMap<>();
        for (Integer numero : lecciones()) {
            if (hayRecreoDespues(numero)) {
                LocalTime inicioRecreo = horaFinLeccion(numero);
                recreos.put(numero, new FranjaHoraria(inicioRecreo,
                        inicioRecreo.plusMinutes(minutosRecreoDespues(numero))));
            }
        }
        return recreos;
    }

    public Map<Integer, FranjaHoraria> almuerzos() {
        Map<Integer, FranjaHoraria> almuerzos = new LinkedHashMap<>();
        for (Integer numero : lecciones()) {
            if (hayAlmuerzoDespues(numero)) {
                LocalTime inicioAlmuerzo = horaFinLeccion(numero);
                almuerzos.put(numero, new FranjaHoraria(inicioAlmuerzo,
                        inicioAlmuerzo.plusMinutes(minutosPausaDespues(numero))));
            }
        }
        return almuerzos;
    }

    public String etiquetaRecreos() {
        List<Integer> duraciones = new ArrayList<>();
        for (Integer numero : lecciones()) {
            if (hayRecreoDespues(numero)) {
                duraciones.add(minutosRecreoDespues(numero));
            }
        }
        if (duraciones.isEmpty()) {
            return "sin recreos";
        }
        if (duraciones.stream().distinct().count() == 1) {
            return "recreo de " + duraciones.get(0) + " min entre bloques";
        }
        if (duraciones.size() == 2) {
            return "recreos de " + duraciones.get(0) + " y " + duraciones.get(1) + " min";
        }
        String cuerpo = duraciones.subList(0, duraciones.size() - 1).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return "recreos de " + cuerpo + " y " + duraciones.get(duraciones.size() - 1) + " min";
    }

    public String serializarBloques() {
        return serializarBloques(bloques);
    }

    static List<BloqueJornada> reconstruir(Integer cantidadLecciones, Integer leccionesPorBloque,
            Integer leccionAlmuerzo, Integer minutosAlmuerzo, Integer minutosRecreo,
            List<Integer> duracionesRecreos) {
        int cantidad = cantidadLecciones == null ? 0 : cantidadLecciones;
        int tamano = Objects.requireNonNullElse(leccionesPorBloque, LECCIONES_POR_BLOQUE_PREDETERMINADAS);
        int minutosAlmuerzoEfectivos = Objects.requireNonNullElse(minutosAlmuerzo, MINUTOS_ALMUERZO_PREDETERMINADOS);
        int leccionAlmuerzoEfectiva = Objects.requireNonNullElse(leccionAlmuerzo, LECCION_ALMUERZO_PREDETERMINADA);
        int minutosRecreoEfectivos = Objects.requireNonNullElse(minutosRecreo, MINUTOS_RECREO_PREDETERMINADOS);
        List<Integer> duraciones = duracionesRecreos == null ? List.of() : duracionesRecreos;
        List<BloqueJornada> reconstruidos = new ArrayList<>();
        int lecciones = 0;
        int indiceRecreo = 0;
        for (int numero = 1; numero <= cantidad; numero++) {
            lecciones++;
            boolean ultimo = numero == cantidad;
            boolean almuerzo = !ultimo && minutosAlmuerzoEfectivos > 0 && leccionAlmuerzoEfectiva == numero;
            boolean receso = !ultimo && !almuerzo && tamano > 0 && numero % tamano == 0;
            if (!ultimo && !almuerzo && !receso) {
                continue;
            }
            TipoPausa pausa = TipoPausa.NINGUNA;
            int minutos = 0;
            if (almuerzo) {
                pausa = TipoPausa.ALMUERZO;
                minutos = minutosAlmuerzoEfectivos;
            } else if (receso) {
                minutos = minutosRecreoEnIndice(indiceRecreo++, duraciones, minutosRecreoEfectivos);
                if (minutos > 0) {
                    pausa = TipoPausa.RECESO;
                }
            }
            reconstruidos.add(new BloqueJornada(lecciones, pausa, minutos));
            lecciones = 0;
        }
        if (lecciones > 0) {
            reconstruidos.add(new BloqueJornada(lecciones, TipoPausa.NINGUNA, 0));
        }
        return reconstruidos;
    }

    private static int minutosRecreoEnIndice(int indice, List<Integer> duraciones, int respaldo) {
        if (indice >= 0 && indice < duraciones.size()) {
            return duraciones.get(indice);
        }
        return respaldo;
    }

    private int minutosPausaDespues(int numeroLeccion) {
        BloqueJornada bloque = pausaTras(numeroLeccion);
        if (bloque != null && (bloque.esAlmuerzo() || bloque.esReceso())) {
            return bloque.minutos();
        }
        return 0;
    }

    private int[] minutosPausaPorLeccion() {
        int total = cantidadLecciones();
        int[] pausas = new int[total + 1];
        int acumulado = 0;
        for (int i = 0; i < bloques.size(); i++) {
            BloqueJornada bloque = bloques.get(i);
            acumulado += bloque.lecciones();
            if (i == bloques.size() - 1 || acumulado >= total) {
                continue;
            }
            if (bloque.esReceso() || bloque.esAlmuerzo()) {
                pausas[acumulado] = bloque.minutos();
            }
        }
        return pausas;
    }

    private BloqueJornada pausaTras(int numeroLeccion) {
        int acumulado = 0;
        for (int i = 0; i < bloques.size(); i++) {
            BloqueJornada bloque = bloques.get(i);
            acumulado += bloque.lecciones();
            if (acumulado == numeroLeccion) {
                return i == bloques.size() - 1 ? null : bloque;
            }
            if (acumulado > numeroLeccion) {
                return null;
            }
        }
        return null;
    }
}
