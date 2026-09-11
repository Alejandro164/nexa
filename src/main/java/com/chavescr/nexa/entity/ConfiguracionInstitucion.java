package com.chavescr.nexa.entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuraciones_institucion")
public class ConfiguracionInstitucion {

    public static final List<String> DIAS_CATALOGO = List.of(
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");
    public static final List<String> DIAS_PREDETERMINADOS = List.of(
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES");

    public static String etiquetaDia(String dia) {
        return switch (dia) {
            case "LUNES" -> "Lunes";
            case "MARTES" -> "Martes";
            case "MIERCOLES" -> "Miércoles";
            case "JUEVES" -> "Jueves";
            case "VIERNES" -> "Viernes";
            case "SABADO" -> "Sábado";
            default -> dia;
        };
    }

    public static String etiquetaCorta(String dia) {
        return switch (dia) {
            case "LUNES" -> "Lun";
            case "MARTES" -> "Mar";
            case "MIERCOLES" -> "Mié";
            case "JUEVES" -> "Jue";
            case "VIERNES" -> "Vie";
            case "SABADO" -> "Sáb";
            default -> dia;
        };
    }

    public static final int CANTIDAD_LECCIONES_PREDETERMINADA = 12;
    public static final LocalTime INICIO_JORNADA_PREDETERMINADO = LocalTime.of(7, 0);
    public static final int MINUTOS_LECCION_PREDETERMINADOS = 40;
    public static final int MINUTOS_RECREO_PREDETERMINADOS = 10;
    public static final List<Integer> DURACIONES_RECREOS_PREDETERMINADAS = List.of(15, 20, 10);
    public static final int LECCIONES_POR_BLOQUE_PREDETERMINADAS = 2;
    public static final int LECCION_ALMUERZO_PREDETERMINADA = 6;
    public static final int MINUTOS_ALMUERZO_PREDETERMINADOS = 40;

    public record FranjaHoraria(LocalTime inicio, LocalTime fin) {
        public LocalTime getInicio() {
            return inicio;
        }

        public LocalTime getFin() {
            return fin;
        }

        public int getMinutos() {
            return (int) java.time.Duration.between(inicio, fin).toMinutes();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false, unique = true)
    private Institucion institucion;

    @Column(nullable = false)
    private Integer cantidadLecciones = CANTIDAD_LECCIONES_PREDETERMINADA;

    @Column(nullable = false)
    private LocalTime inicioJornada = INICIO_JORNADA_PREDETERMINADO;

    @Column(nullable = false)
    private Integer minutosLeccion = MINUTOS_LECCION_PREDETERMINADOS;

    @Column(nullable = false)
    private Integer minutosRecreo = MINUTOS_RECREO_PREDETERMINADOS;

    @Column(name = "duraciones_recreos", length = 80)
    private String duracionesRecreos = DURACIONES_RECREOS_PREDETERMINADAS.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

    @Column(nullable = false)
    private Integer leccionesPorBloque = LECCIONES_POR_BLOQUE_PREDETERMINADAS;

    @Column(name = "leccion_almuerzo")
    private Integer leccionAlmuerzo = LECCION_ALMUERZO_PREDETERMINADA;

    @Column(name = "minutos_almuerzo")
    private Integer minutosAlmuerzo = MINUTOS_ALMUERZO_PREDETERMINADOS;

    @Column(name = "dias_laborales", nullable = false, length = 80)
    private String diasLaborales = String.join(",", DIAS_PREDETERMINADOS);

    public static ConfiguracionInstitucion predeterminada(Institucion institucion) {
        ConfiguracionInstitucion config = new ConfiguracionInstitucion();
        config.setInstitucion(institucion);
        return config;
    }

    public List<String> getDias() {
        if (diasLaborales == null || diasLaborales.isBlank()) {
            return List.of();
        }
        return Arrays.stream(diasLaborales.split(","))
                .map(String::trim)
                .filter(dia -> !dia.isEmpty())
                .toList();
    }

    public void setDias(List<String> dias) {
        List<String> ordenados = new ArrayList<>();
        for (String dia : DIAS_CATALOGO) {
            if (dias != null && dias.contains(dia)) {
                ordenados.add(dia);
            }
        }
        this.diasLaborales = String.join(",", ordenados);
    }

    public List<Integer> getLecciones() {
        int cantidad = cantidadLecciones == null ? 0 : cantidadLecciones;
        return IntStream.rangeClosed(1, cantidad).boxed().toList();
    }

    public LocalTime horaInicioLeccion(int numeroLeccion) {
        LocalTime hora = Objects.requireNonNullElse(inicioJornada, INICIO_JORNADA_PREDETERMINADO);
        int duracion = minutosLeccionEfectivos();
        for (int numero = 1; numero < numeroLeccion; numero++) {
            hora = hora.plusMinutes(duracion + minutosPausaDespues(numero));
        }
        return hora;
    }

    public LocalTime horaFinLeccion(int numeroLeccion) {
        return horaInicioLeccion(numeroLeccion).plusMinutes(minutosLeccionEfectivos());
    }

    public boolean hayAlmuerzoDespues(int numeroLeccion) {
        if (minutosAlmuerzoEfectivos() <= 0 || cantidadLecciones == null || numeroLeccion >= cantidadLecciones) {
            return false;
        }
        return leccionAlmuerzoEfectiva() == numeroLeccion;
    }

    public boolean hayRecreoDespues(int numeroLeccion) {
        return minutosRecreoDespues(numeroLeccion) > 0;
    }

    public int minutosRecreoDespues(int numeroLeccion) {
        if (!esHuecoRecreo(numeroLeccion)) {
            return 0;
        }
        return minutosRecreoEnIndice(indiceHuecoRecreo(numeroLeccion));
    }

    public List<Integer> listaDuracionesRecreos() {
        if (duracionesRecreos == null || duracionesRecreos.isBlank()) {
            return List.of();
        }
        List<Integer> valores = new ArrayList<>();
        for (String parte : duracionesRecreos.split(",")) {
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

    public void setListaDuracionesRecreos(List<Integer> minutos) {
        if (minutos == null || minutos.isEmpty()) {
            this.duracionesRecreos = "";
            return;
        }
        this.duracionesRecreos = minutos.stream()
                .map(valor -> String.valueOf(limitarMinutosRecreo(valor)))
                .collect(Collectors.joining(","));
    }

    public String etiquetaRecreos() {
        List<Integer> duraciones = new ArrayList<>();
        for (Integer numero : getLecciones()) {
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

    private boolean esHuecoRecreo(int numeroLeccion) {
        if (hayAlmuerzoDespues(numeroLeccion)) {
            return false;
        }
        int bloque = Objects.requireNonNullElse(leccionesPorBloque, LECCIONES_POR_BLOQUE_PREDETERMINADAS);
        return bloque > 0 && cantidadLecciones != null && numeroLeccion < cantidadLecciones
                && numeroLeccion % bloque == 0;
    }

    private int indiceHuecoRecreo(int numeroLeccion) {
        int indice = 0;
        for (int numero = 1; numero < numeroLeccion; numero++) {
            if (esHuecoRecreo(numero)) {
                indice++;
            }
        }
        return indice;
    }

    private int minutosRecreoEnIndice(int indice) {
        List<Integer> duraciones = listaDuracionesRecreos();
        if (indice >= 0 && indice < duraciones.size()) {
            return duraciones.get(indice);
        }
        return minutosRecreoEfectivos();
    }

    private int limitarMinutosRecreo(Integer minutos) {
        int valor = minutos == null ? MINUTOS_RECREO_PREDETERMINADOS : minutos;
        return Math.max(0, Math.min(60, valor));
    }

    private int minutosPausaDespues(int numeroLeccion) {
        if (hayAlmuerzoDespues(numeroLeccion)) {
            return minutosAlmuerzoEfectivos();
        }
        return minutosRecreoDespues(numeroLeccion);
    }

    private int minutosLeccionEfectivos() {
        return Objects.requireNonNullElse(minutosLeccion, MINUTOS_LECCION_PREDETERMINADOS);
    }

    private int minutosRecreoEfectivos() {
        return Objects.requireNonNullElse(minutosRecreo, MINUTOS_RECREO_PREDETERMINADOS);
    }

    private int minutosAlmuerzoEfectivos() {
        return Objects.requireNonNullElse(minutosAlmuerzo, MINUTOS_ALMUERZO_PREDETERMINADOS);
    }

    private int leccionAlmuerzoEfectiva() {
        return Objects.requireNonNullElse(leccionAlmuerzo, LECCION_ALMUERZO_PREDETERMINADA);
    }

    public Map<Integer, FranjaHoraria> franjas() {
        Map<Integer, FranjaHoraria> franjas = new LinkedHashMap<>();
        for (Integer numero : getLecciones()) {
            franjas.put(numero, new FranjaHoraria(horaInicioLeccion(numero), horaFinLeccion(numero)));
        }
        return franjas;
    }

    public Map<Integer, FranjaHoraria> recreos() {
        Map<Integer, FranjaHoraria> recreos = new LinkedHashMap<>();
        for (Integer numero : getLecciones()) {
            if (hayRecreoDespues(numero)) {
                LocalTime inicio = horaFinLeccion(numero);
                recreos.put(numero, new FranjaHoraria(inicio, inicio.plusMinutes(minutosRecreoDespues(numero))));
            }
        }
        return recreos;
    }

    public Map<Integer, FranjaHoraria> almuerzos() {
        Map<Integer, FranjaHoraria> almuerzos = new LinkedHashMap<>();
        for (Integer numero : getLecciones()) {
            if (hayAlmuerzoDespues(numero)) {
                LocalTime inicio = horaFinLeccion(numero);
                almuerzos.put(numero, new FranjaHoraria(inicio, inicio.plusMinutes(minutosAlmuerzoEfectivos())));
            }
        }
        return almuerzos;
    }

    public void aplicarHorario(HorarioLeccion leccion) {
        leccion.setHoraInicio(horaInicioLeccion(leccion.getNumeroLeccion()));
        leccion.setHoraFin(horaFinLeccion(leccion.getNumeroLeccion()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Institucion getInstitucion() {
        return institucion;
    }

    public void setInstitucion(Institucion institucion) {
        this.institucion = institucion;
    }

    public Integer getCantidadLecciones() {
        return cantidadLecciones;
    }

    public void setCantidadLecciones(Integer cantidadLecciones) {
        this.cantidadLecciones = cantidadLecciones;
    }

    public LocalTime getInicioJornada() {
        return inicioJornada;
    }

    public void setInicioJornada(LocalTime inicioJornada) {
        this.inicioJornada = inicioJornada;
    }

    public Integer getMinutosLeccion() {
        return minutosLeccion;
    }

    public void setMinutosLeccion(Integer minutosLeccion) {
        this.minutosLeccion = minutosLeccion;
    }

    public Integer getMinutosRecreo() {
        return minutosRecreo;
    }

    public void setMinutosRecreo(Integer minutosRecreo) {
        this.minutosRecreo = minutosRecreo;
    }

    public String getDuracionesRecreos() {
        return duracionesRecreos;
    }

    public void setDuracionesRecreos(String duracionesRecreos) {
        this.duracionesRecreos = duracionesRecreos;
    }

    public Integer getLeccionesPorBloque() {
        return leccionesPorBloque;
    }

    public void setLeccionesPorBloque(Integer leccionesPorBloque) {
        this.leccionesPorBloque = leccionesPorBloque;
    }

    public Integer getLeccionAlmuerzo() {
        return leccionAlmuerzo;
    }

    public void setLeccionAlmuerzo(Integer leccionAlmuerzo) {
        this.leccionAlmuerzo = leccionAlmuerzo;
    }

    public Integer getMinutosAlmuerzo() {
        return minutosAlmuerzo;
    }

    public void setMinutosAlmuerzo(Integer minutosAlmuerzo) {
        this.minutosAlmuerzo = minutosAlmuerzo;
    }

    public String getDiasLaborales() {
        return diasLaborales;
    }

    public void setDiasLaborales(String diasLaborales) {
        this.diasLaborales = diasLaborales;
    }
}
