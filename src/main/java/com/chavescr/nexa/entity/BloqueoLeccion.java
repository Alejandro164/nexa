package com.chavescr.nexa.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bloqueo_lecciones")
public class BloqueoLeccion {

    public static final String MODO_CERRADA = "cerrada";
    public static final String MODO_TIPO = "tipo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @Column(nullable = false, length = 80)
    private String lecciones;

    @Column(nullable = false, length = 80)
    private String dias;

    @Column(nullable = false, length = 40)
    private String grados;

    @Column(nullable = false, length = 12)
    private String modo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_materia_id")
    private TipoMateria tipoMateria;

    @Column(length = 160)
    private String motivo;

    public boolean aplica(int grado, String dia, int numeroLeccion) {
        return listaGrados().contains(grado)
                && listaDias().contains(dia)
                && listaLecciones().contains(numeroLeccion);
    }

    public boolean estaCerrada() {
        return MODO_CERRADA.equals(modo);
    }

    public boolean permite(TipoMateria tipo) {
        if (estaCerrada() || tipoMateria == null || tipo == null) {
            return false;
        }
        return Objects.equals(tipoMateria.getId(), tipo.getId());
    }

    public boolean solapaCon(BloqueoLeccion otra) {
        if (otra == null) {
            return false;
        }
        return intersecan(listaGrados(), otra.listaGrados())
                && intersecan(listaDias(), otra.listaDias())
                && intersecan(listaLecciones(), otra.listaLecciones());
    }

    public static BloqueoLeccion vigente(List<BloqueoLeccion> reglas, int grado, String dia, int numeroLeccion) {
        if (reglas == null) {
            return null;
        }
        for (BloqueoLeccion regla : reglas) {
            if (regla.aplica(grado, dia, numeroLeccion)) {
                return regla;
            }
        }
        return null;
    }

    public static void exigirSinSolapes(List<BloqueoLeccion> reglas) {
        if (reglas == null || reglas.size() < 2) {
            return;
        }
        for (int i = 0; i < reglas.size(); i++) {
            for (int j = i + 1; j < reglas.size(); j++) {
                if (reglas.get(i).solapaCon(reglas.get(j))) {
                    throw new IllegalArgumentException(mensajeSolape(reglas.get(i), reglas.get(j)));
                }
            }
        }
    }

    public List<Integer> listaLecciones() {
        return parsearEnteros(lecciones);
    }

    public List<String> listaDias() {
        return DiaLaboral.parsear(dias);
    }

    public List<Integer> listaGrados() {
        return parsearEnteros(grados);
    }

    public void setListaLecciones(List<Integer> numeros) {
        this.lecciones = serializarEnteros(numeros);
    }

    public void setListaDias(List<String> codigos) {
        this.dias = DiaLaboral.serializar(codigos);
    }

    public void setListaGrados(List<Integer> numeros) {
        this.grados = serializarEnteros(numeros);
    }

    static List<Integer> parsearEnteros(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }

    static String serializarEnteros(List<Integer> numeros) {
        if (numeros == null || numeros.isEmpty()) {
            return "";
        }
        return numeros.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    static <T> boolean intersecan(List<T> a, List<T> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        for (T valor : a) {
            if (b.contains(valor)) {
                return true;
            }
        }
        return false;
    }

    static String mensajeSolape(BloqueoLeccion a, BloqueoLeccion b) {
        for (Integer grado : a.listaGrados()) {
            for (String dia : a.listaDias()) {
                for (Integer leccion : a.listaLecciones()) {
                    if (b.aplica(grado, dia, leccion)) {
                        return "La lección " + leccion + " del "
                                + DiaLaboral.etiqueta(dia).toLowerCase()
                                + " en grado " + grado
                                + " ya está bloqueada. Quítala de las reglas activas si quieres cambiarla.";
                    }
                }
            }
        }
        return "Esa lección ya está bloqueada. Quítala de las reglas activas si quieres cambiarla.";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Institucion getInstitucion() { return institucion; }
    public void setInstitucion(Institucion institucion) { this.institucion = institucion; }
    public String getLecciones() { return lecciones; }
    public void setLecciones(String lecciones) { this.lecciones = lecciones; }
    public String getDias() { return dias; }
    public void setDias(String dias) { this.dias = dias; }
    public String getGrados() { return grados; }
    public void setGrados(String grados) { this.grados = grados; }
    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public TipoMateria getTipoMateria() { return tipoMateria; }
    public void setTipoMateria(TipoMateria tipoMateria) { this.tipoMateria = tipoMateria; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
