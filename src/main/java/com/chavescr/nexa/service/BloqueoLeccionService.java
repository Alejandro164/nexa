package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.BloqueoLeccion;
import com.chavescr.nexa.entity.ConfiguracionDireccion;
import com.chavescr.nexa.entity.DiaLaboral;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.repository.BloqueoLeccionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class BloqueoLeccionService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<Regla>> TIPO_REGLAS = new TypeReference<>() { };

    private final BloqueoLeccionRepository bloqueoRepository;
    private final DireccionRepository direccionRepository;
    private final TipoMateriaRepository tipoMateriaRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final ConfiguracionDireccionService configuracionDireccionService;

    public BloqueoLeccionService(BloqueoLeccionRepository bloqueoRepository,
            DireccionRepository direccionRepository,
            TipoMateriaRepository tipoMateriaRepository,
            NivelAcademicoRepository nivelRepository,
            ConfiguracionDireccionService configuracionDireccionService) {
        this.bloqueoRepository = bloqueoRepository;
        this.direccionRepository = direccionRepository;
        this.tipoMateriaRepository = tipoMateriaRepository;
        this.nivelRepository = nivelRepository;
        this.configuracionDireccionService = configuracionDireccionService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String estadoJson(Long direccionId) {
        try {
            return MAPPER.writeValueAsString(estado(direccionId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo preparar el bloqueo de lección", e);
        }
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public BloqueoLeccion vigente(Long direccionId, int grado, String dia, int numeroLeccion) {
        return BloqueoLeccion.vigente(
                bloqueoRepository.findByDireccionIdOrderByIdAsc(direccionId),
                grado, dia, numeroLeccion);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Materia> filtrarMaterias(Long direccionId, int grado, String dia, int numeroLeccion,
            List<Materia> materias, Long materiaActualId) {
        BloqueoLeccion regla = vigente(direccionId, grado, dia, numeroLeccion);
        if (regla == null) {
            return materias;
        }
        if (regla.estaCerrada()) {
            return List.of();
        }
        return materias.stream()
                .filter(materia -> regla.permite(materia.getTipoMateria())
                        || Objects.equals(materia.getId(), materiaActualId))
                .toList();
    }

    public void validarAsignacion(Long direccionId, int grado, String dia, int numeroLeccion, Materia materia) {
        BloqueoLeccion regla = vigente(direccionId, grado, dia, numeroLeccion);
        if (regla == null) {
            return;
        }
        if (regla.estaCerrada()) {
            throw new IllegalArgumentException("Esta lección no se puede usar en el horario");
        }
        if (!regla.permite(materia.getTipoMateria())) {
            String tipo = regla.getTipoMateria() != null ? regla.getTipoMateria().getNombre() : "restringido";
            throw new IllegalArgumentException("Esta lección solo admite materias de tipo " + tipo);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void guardar(Long direccionId, String reglasJson) {
        List<Regla> reglas = parsear(reglasJson);
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
        List<BloqueoLeccion> entidades = new ArrayList<>();
        for (Regla regla : reglas) {
            entidades.add(aEntidad(direccion, regla));
        }
        BloqueoLeccion.exigirSinSolapes(entidades);
        bloqueoRepository.deleteByDireccionId(direccionId);
        bloqueoRepository.flush();
        bloqueoRepository.saveAll(entidades);
        bloqueoRepository.flush();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String reglasJson(Long direccionId) {
        try {
            return MAPPER.writeValueAsString(bloqueoRepository
                    .findByDireccionIdOrderByIdAsc(direccionId)
                    .stream()
                    .map(this::aVista)
                    .toList());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo preparar el bloqueo de lección", e);
        }
    }

    private Map<String, Object> estado(Long direccionId) {
        ConfiguracionDireccion config = configuracionDireccionService.obtener(direccionId);
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("numeros", config.getLecciones());
        estado.put("dias", config.getDias().stream()
                .map(dia -> Map.of("codigo", dia, "corto", DiaLaboral.etiquetaCorta(dia)))
                .toList());
        estado.put("tipos", tipoMateriaRepository.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
                .map(tipo -> Map.of("codigo", tipo.getCodigo(), "nombre", tipo.getNombre()))
                .toList());
        estado.put("grados", gradosDeLaDireccion(direccionId));
        estado.put("reglas", bloqueoRepository.findByDireccionIdOrderByIdAsc(direccionId).stream()
                .map(this::aVista)
                .toList());
        return estado;
    }

    private List<Integer> gradosDeLaDireccion(Long direccionId) {
        return nivelRepository
                .findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId)
                .stream()
                .map(NivelAcademico::getGrado)
                .distinct()
                .toList();
    }

    private Map<String, Object> aVista(BloqueoLeccion bloqueo) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("id", bloqueo.getId());
        vista.put("lecciones", bloqueo.listaLecciones());
        vista.put("dias", bloqueo.listaDias());
        vista.put("grados", bloqueo.listaGrados());
        vista.put("modo", bloqueo.getModo());
        vista.put("tipoCodigo", bloqueo.getTipoMateria() != null ? bloqueo.getTipoMateria().getCodigo() : null);
        vista.put("motivo", bloqueo.getMotivo() == null ? "" : bloqueo.getMotivo());
        return vista;
    }

    private BloqueoLeccion aEntidad(Direccion direccion, Regla regla) {
        if (regla.lecciones() == null || regla.lecciones().isEmpty()
                || regla.dias() == null || regla.dias().isEmpty()
                || regla.grados() == null || regla.grados().isEmpty()) {
            throw new IllegalArgumentException("Cada bloqueo necesita lecciones, días y grados");
        }
        for (String dia : regla.dias()) {
            if (!DiaLaboral.CATALOGO.contains(dia)) {
                throw new IllegalArgumentException("Día laboral inválido");
            }
        }
        BloqueoLeccion entidad = new BloqueoLeccion();
        entidad.setDireccion(direccion);
        entidad.setListaLecciones(regla.lecciones());
        entidad.setListaDias(regla.dias());
        entidad.setListaGrados(regla.grados());
        String motivo = regla.motivo() == null ? null : regla.motivo().trim();
        if (motivo != null && motivo.length() > 160) {
            motivo = motivo.substring(0, 160);
        }
        entidad.setMotivo(motivo == null || motivo.isEmpty() ? null : motivo);
        if (BloqueoLeccion.MODO_CERRADA.equals(regla.modo())) {
            entidad.setModo(BloqueoLeccion.MODO_CERRADA);
            return entidad;
        }
        if (!BloqueoLeccion.MODO_TIPO.equals(regla.modo()) || regla.tipoCodigo() == null || regla.tipoCodigo().isBlank()) {
            throw new IllegalArgumentException("Indica el tipo de materia permitido");
        }
        TipoMateria tipo = tipoMateriaRepository.findByCodigo(regla.tipoCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de materia inválido"));
        entidad.setModo(BloqueoLeccion.MODO_TIPO);
        entidad.setTipoMateria(tipo);
        return entidad;
    }

    private List<Regla> parsear(String reglasJson) {
        if (reglasJson == null || reglasJson.isBlank()) {
            throw new IllegalArgumentException("No se recibieron los bloqueos");
        }
        String json = reglasJson.trim();
        if ("[]".equals(json)) {
            return List.of();
        }
        try {
            List<Regla> reglas = MAPPER.readValue(json, TIPO_REGLAS);
            return reglas == null ? List.of() : reglas;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudieron leer los bloqueos");
        }
    }

    public record Regla(
            List<Integer> lecciones,
            List<String> dias,
            List<Integer> grados,
            String modo,
            String tipoCodigo,
            String motivo) {
    }
}
