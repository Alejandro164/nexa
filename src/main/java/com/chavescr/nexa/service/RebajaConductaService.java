package com.chavescr.nexa.service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.DestinoRebajaAsistencia;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.QuienRegistraPuntosBoleta;
import com.chavescr.nexa.entity.RebajaConducta;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.RebajaConductaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class RebajaConductaService {

    private static final int CADA_MIN = 1;
    private static final int CADA_MAX = 30;
    private static final int PUNTOS_MIN = 0;
    private static final int PUNTOS_MAX = 100;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RebajaConductaRepository repository;
    private final DireccionRepository direccionRepository;

    public RebajaConductaService(RebajaConductaRepository repository, DireccionRepository direccionRepository) {
        this.repository = repository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public RebajaConducta obtener(Long direccionId) {
        return repository.findByDireccionId(direccionId).orElseGet(RebajaConducta::predeterminada);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean asistenciaRebajaComponente(Long direccionId) {
        return obtener(direccionId).getDestinoRebajaAsistencia() == DestinoRebajaAsistencia.COMPONENTE;
    }

    /** Reglas listas para restar de la nota. Si el texto guardado no se puede leer, usa las predeterminadas. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CalculoRebaja calculo(Long direccionId) {
        RebajaConducta rebaja = obtener(direccionId);
        Map<String, JsonNode> porId = leerOPredeterminadas(rebaja.getReglas());
        Map<TipoRebaja, int[]> reglas = new EnumMap<>(TipoRebaja.class);
        for (TipoRebaja tipo : TipoRebaja.values()) {
            reglas.put(tipo, reglaDe(porId.get(tipo.id()), tipo));
        }
        boolean enConducta = rebaja.getDestinoRebajaAsistencia() != DestinoRebajaAsistencia.COMPONENTE;
        return new CalculoRebaja(reglas, enConducta);
    }

    @Transactional(rollbackFor = Exception.class)
    public RebajaConducta guardar(Long direccionId, String reglasJson,
            QuienRegistraPuntosBoleta quienRegistraPuntosBoleta,
            DestinoRebajaAsistencia destinoRebajaAsistencia) {
        String canonico = canonizar(reglasJson);
        if (quienRegistraPuntosBoleta == null) {
            throw new IllegalArgumentException("Indica quién registra los puntos de las boletas");
        }
        if (destinoRebajaAsistencia == null) {
            throw new IllegalArgumentException("Indica dónde se rebajan las ausencias y tardías");
        }
        RebajaConducta rebaja = repository.findByDireccionId(direccionId).orElseGet(() -> nueva(direccionId));
        rebaja.setReglas(canonico);
        rebaja.setQuienRegistraPuntosBoleta(quienRegistraPuntosBoleta);
        rebaja.setDestinoRebajaAsistencia(destinoRebajaAsistencia);
        return repository.save(rebaja);
    }

    static String canonizar(String reglasJson) {
        Map<String, JsonNode> porId = leer(reglasJson);
        StringBuilder out = new StringBuilder("[");
        for (TipoRebaja tipo : TipoRebaja.values()) {
            JsonNode nodo = porId.remove(tipo.id());
            if (nodo == null) {
                throw new IllegalArgumentException("Falta la rebaja de «" + tipo.nombre() + "»");
            }
            int cada = entero(nodo.get("cada"), "«" + tipo.nombre() + "» se cuenta de 1 a 30");
            int puntos = entero(nodo.get("puntos"), "Los puntos de «" + tipo.nombre() + "» van de 0 a 100");
            if (cada < CADA_MIN || cada > CADA_MAX) {
                throw new IllegalArgumentException("«" + tipo.nombre() + "» se cuenta de 1 a 30");
            }
            if (puntos < PUNTOS_MIN || puntos > PUNTOS_MAX) {
                throw new IllegalArgumentException("Los puntos de «" + tipo.nombre() + "» van de 0 a 100");
            }
            if (out.length() > 1) {
                out.append(',');
            }
            out.append("{\"id\":\"").append(tipo.id())
                    .append("\",\"cada\":").append(cada)
                    .append(",\"puntos\":").append(puntos)
                    .append('}');
        }
        if (!porId.isEmpty()) {
            throw new IllegalArgumentException("Hay una rebaja que no corresponde");
        }
        String canonico = out.append(']').toString();
        if (canonico.length() > RebajaConducta.MAX_REGLAS) {
            throw new IllegalArgumentException("Las rebajas no caben");
        }
        return canonico;
    }

    private RebajaConducta nueva(Long direccionId) {
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
        RebajaConducta rebaja = new RebajaConducta();
        rebaja.setDireccion(direccion);
        return rebaja;
    }

    private static Map<String, JsonNode> leerOPredeterminadas(String json) {
        try {
            return leer(json == null || json.isBlank() ? RebajaConducta.REGLAS_PREDETERMINADAS : json);
        } catch (IllegalArgumentException e) {
            return leer(RebajaConducta.REGLAS_PREDETERMINADAS);
        }
    }

    private static int[] reglaDe(JsonNode nodo, TipoRebaja tipo) {
        if (nodo == null) {
            return new int[] { tipo.cada(), tipo.puntos() };
        }
        JsonNode cada = nodo.get("cada");
        JsonNode puntos = nodo.get("puntos");
        int cadaValor = cada != null && cada.canConvertToInt() ? cada.intValue() : tipo.cada();
        int puntosValor = puntos != null && puntos.canConvertToInt() ? puntos.intValue() : tipo.puntos();
        return new int[] { cadaValor, puntosValor };
    }

    private static Map<String, JsonNode> leer(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Indica las rebajas antes de guardarlas");
        }
        JsonNode root;
        try {
            root = JSON.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Las rebajas no se pudieron leer");
        }
        if (!root.isArray()) {
            throw new IllegalArgumentException("Las rebajas no se pudieron leer");
        }
        Map<String, JsonNode> porId = new LinkedHashMap<>();
        for (JsonNode nodo : root) {
            if (nodo == null || !nodo.isObject()) {
                throw new IllegalArgumentException("Las rebajas no se pudieron leer");
            }
            JsonNode id = nodo.get("id");
            if (id == null || !id.isTextual() || id.asText().isBlank()) {
                throw new IllegalArgumentException("Cada rebaja necesita un registro");
            }
            TipoRebaja tipo = TipoRebaja.porId(id.asText());
            if (tipo == null) {
                throw new IllegalArgumentException("Hay una rebaja que no corresponde");
            }
            if (porId.put(tipo.id(), nodo) != null) {
                throw new IllegalArgumentException("La rebaja de «" + tipo.nombre() + "» está repetida");
            }
        }
        return porId;
    }

    private static int entero(JsonNode nodo, String mensaje) {
        if (nodo == null || !nodo.isNumber() || !nodo.canConvertToInt()
                || nodo.doubleValue() != nodo.intValue()) {
            throw new IllegalArgumentException(mensaje);
        }
        return nodo.intValue();
    }
}
