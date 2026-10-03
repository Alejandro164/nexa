package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.VistaEscala;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.EscalaNotas;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.EscalaNotasRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class EscalaNotasService {

    private static final int MIN_TRAMOS = 2;
    private static final int MAX_TRAMOS = 5;
    private static final int MAX_NOMBRE = 24;
    private static final Locale ES = Locale.forLanguageTag("es");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final EscalaNotasRepository repository;
    private final DireccionRepository direccionRepository;

    public EscalaNotasService(EscalaNotasRepository repository, DireccionRepository direccionRepository) {
        this.repository = repository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public EscalaNotas obtener(Long direccionId) {
        return repository.findByDireccionId(direccionId).orElseGet(EscalaNotas::predeterminada);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public VistaEscala vista(Long direccionId) {
        return vista(obtener(direccionId));
    }

    static VistaEscala vista(EscalaNotas escala) {
        List<Tramo> tramos;
        try {
            tramos = normalizar(escala.getNotaMinima(), escala.getNotaMaxima(), escala.getDecimales(),
                    escala.getNotaAprobacion(), leer(escala.getTramos()));
        } catch (RuntimeException e) {
            return vista(EscalaNotas.predeterminada());
        }
        int corte = 0;
        while (tramos.get(corte).desde() != escala.getNotaAprobacion()) {
            corte++;
        }
        List<VistaEscala.Tramo> out = new ArrayList<>();
        for (int i = 0; i < tramos.size(); i++) {
            out.add(new VistaEscala.Tramo(tramos.get(i).nombre(), tramos.get(i).desde(), tonoDe(i, corte)));
        }
        return new VistaEscala(out);
    }

    @Transactional(rollbackFor = Exception.class)
    public EscalaNotas guardar(Long direccionId, Integer notaMinima, Integer notaMaxima,
            Integer decimales, Integer notaAprobacion, String tramosJson) {
        String tramos = canonizar(notaMinima, notaMaxima, decimales, notaAprobacion, tramosJson);
        EscalaNotas escala = repository.findByDireccionId(direccionId).orElseGet(() -> nueva(direccionId));
        escala.setNotaMinima(notaMinima);
        escala.setNotaMaxima(notaMaxima);
        escala.setDecimales(decimales);
        escala.setNotaAprobacion(notaAprobacion);
        escala.setTramos(tramos);
        return repository.save(escala);
    }

    static String canonizar(Integer notaMinima, Integer notaMaxima, Integer decimales,
            Integer notaAprobacion, String tramosJson) {
        if (notaMinima == null || notaMaxima == null || decimales == null || notaAprobacion == null) {
            throw new IllegalArgumentException("Completa la escala antes de guardarla");
        }
        List<Tramo> tramos = normalizar(notaMinima, notaMaxima, decimales, notaAprobacion, leer(tramosJson));
        String canonico = serializar(tramos);
        if (canonico.length() > 800) {
            throw new IllegalArgumentException("La escala de tramos es demasiado larga");
        }
        return canonico;
    }

    private EscalaNotas nueva(Long direccionId) {
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
        EscalaNotas escala = new EscalaNotas();
        escala.setDireccion(direccion);
        return escala;
    }

    private static List<Tramo> leer(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Indica los tramos de la escala");
        }
        JsonNode root;
        try {
            root = JSON.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("La escala de tramos no se pudo leer");
        }
        if (!root.isArray()) {
            throw new IllegalArgumentException("La escala de tramos no se pudo leer");
        }
        List<Tramo> tramos = new ArrayList<>();
        for (JsonNode nodo : root) {
            if (nodo == null || !nodo.isObject()) {
                throw new IllegalArgumentException("La escala de tramos no se pudo leer");
            }
            JsonNode desde = nodo.get("desde");
            if (desde == null || !desde.isNumber() || !desde.canConvertToInt()
                    || desde.doubleValue() != desde.intValue()) {
                throw new IllegalArgumentException("Cada tramo necesita un inicio entero");
            }
            JsonNode nombre = nodo.get("nombre");
            tramos.add(new Tramo(nombre == null || !nombre.isTextual() ? "" : nombre.asText(), desde.intValue()));
        }
        return tramos;
    }

    private static List<Tramo> normalizar(int min, int max, int decimales, int aprobacion, List<Tramo> crudos) {
        if (min < 0 || min > 99) {
            throw new IllegalArgumentException("La nota mínima va de 0 a 99");
        }
        if (max < 1 || max > 100) {
            throw new IllegalArgumentException("La nota máxima va de 1 a 100");
        }
        if (min >= max) {
            throw new IllegalArgumentException("La nota mínima tiene que ser menor que la máxima");
        }
        if (decimales != 0 && decimales != 1) {
            throw new IllegalArgumentException("Los decimales del promedio son 0 o 1");
        }
        if (crudos.size() < MIN_TRAMOS) {
            throw new IllegalArgumentException("Hacen falta al menos dos tramos");
        }
        if (crudos.size() > MAX_TRAMOS) {
            throw new IllegalArgumentException("Cinco tramos es el máximo");
        }
        if (max - min < crudos.size()) {
            throw new IllegalArgumentException("Entre la mínima y la máxima no caben estos tramos");
        }

        List<Tramo> tramos = new ArrayList<>();
        Set<String> nombres = new HashSet<>();
        for (Tramo crudo : crudos) {
            String nombre = crudo.nombre() == null ? "" : crudo.nombre().trim();
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("Cada tramo necesita un nombre");
            }
            if (nombre.length() > MAX_NOMBRE) {
                throw new IllegalArgumentException("El nombre del tramo no puede superar 24 caracteres");
            }
            if (!nombres.add(nombre.toLowerCase(ES))) {
                throw new IllegalArgumentException("Los nombres de los tramos no se pueden repetir");
            }
            tramos.add(new Tramo(nombre, crudo.desde()));
        }

        int previo = max;
        boolean corteValido = false;
        for (int i = 0; i < tramos.size(); i++) {
            int desde = tramos.get(i).desde();
            boolean piso = i == tramos.size() - 1;
            if (piso) {
                if (desde != min) {
                    throw new IllegalArgumentException("El tramo más bajo tiene que empezar en la nota mínima");
                }
            } else if (!(desde < previo && desde > tramos.get(i + 1).desde())) {
                throw new IllegalArgumentException("Cada inicio tiene que quedar entre el tramo de abajo y el de arriba");
            }
            if (!piso && desde == aprobacion) {
                corteValido = true;
            }
            previo = desde;
        }
        if (!corteValido) {
            throw new IllegalArgumentException("Elige el tramo desde el que se aprueba");
        }
        return tramos;
    }

    /** Mismo criterio que la vista de la escala: el tono sigue al corte, no al nombre. */
    private static String tonoDe(int indice, int corte) {
        if (indice > corte) {
            return "aplazado";
        }
        if (indice == 0) {
            return "excelente";
        }
        return indice == corte ? "regular" : "bueno";
    }

    private static String serializar(List<Tramo> tramos) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < tramos.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            Tramo tramo = tramos.get(i);
            out.append("{\"nombre\":\"").append(escapar(tramo.nombre()))
                    .append("\",\"desde\":").append(tramo.desde()).append('}');
        }
        return out.append(']').toString();
    }

    private static String escapar(String texto) {
        StringBuilder out = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        throw new IllegalArgumentException("El nombre del tramo tiene un carácter inválido");
                    }
                    out.append(c);
                }
            }
        }
        return out.toString();
    }

    private record Tramo(String nombre, int desde) {
    }
}
