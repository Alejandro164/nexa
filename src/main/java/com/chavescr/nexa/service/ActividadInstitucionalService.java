package com.chavescr.nexa.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chavescr.nexa.dto.EventoMepDTO;
import com.chavescr.nexa.entity.ActividadInstitucionalPropia;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ActividadInstitucionalPropiaRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Combina el calendario oficial de actividades del MEP
 * (https://calendario.mep.go.cr/{anio}/webservices/obtener_eventos.php)
 * con las actividades institucionales creadas manualmente (Director/Admin)
 * para llenar la sección "Actividades Institucionales" de la Agenda.
 */
@Service
@Transactional
public class ActividadInstitucionalService {

    private static final Logger log = LoggerFactory.getLogger(ActividadInstitucionalService.class);
    private static final String URL_EVENTOS = "https://calendario.mep.go.cr/%d/webservices/obtener_eventos.php";

    public static final Map<Integer, String> MESES = new LinkedHashMap<>();
    static {
        String[] nombres = { "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre" };
        for (int i = 0; i < nombres.length; i++) {
            MESES.put(i + 1, nombres[i]);
        }
    }

    /** Cuánto tiempo se conserva en memoria la respuesta del MEP antes de volver a consultarla. */
    private static final Duration DURACION_CACHE_MEP = Duration.ofMinutes(15);

    private record EntradaCacheMep(List<EventoMepDTO> eventos, Instant expiracion) {
        boolean vigente() {
            return Instant.now().isBefore(expiracion);
        }
    }

    private final RestClient restClient;
    private final ActividadInstitucionalPropiaRepository actividadPropiaRepository;
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Caché propia en memoria (independiente de {@code @Cacheable}/Redis, que en el perfil "dev"
     * está deshabilitado con {@code spring.cache.type=none}). Sin esto, cada clic en "Ver detalles"
     * o cada navegación del calendario volvía a pedir todo el año completo al MEP.
     */
    private final Map<Integer, EntradaCacheMep> cacheEventosMep = new ConcurrentHashMap<>();

    public ActividadInstitucionalService(ActividadInstitucionalPropiaRepository actividadPropiaRepository,
            InstitucionRepository institucionRepository, UsuarioRepository usuarioRepository) {
        this.actividadPropiaRepository = actividadPropiaRepository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(8000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .configureMessageConverters(clientBuilder -> clientBuilder
                        .disableDefaults()
                        .withJsonConverter(new MappingJackson2HttpMessageConverter(mapper)))
                .build();
    }

    @Cacheable(value = "eventosMep", key = "#anio")
    public List<EventoMepDTO> listarEventosDelAnio(int anio) {
        EntradaCacheMep entrada = cacheEventosMep.get(anio);
        if (entrada != null && entrada.vigente()) {
            return entrada.eventos();
        }
        try {
            List<EventoMepDTO> eventos = restClient.get()
                    .uri(URL_EVENTOS.formatted(anio))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EventoMepDTO>>() { });
            List<EventoMepDTO> resultado = eventos != null ? eventos : List.of();
            cacheEventosMep.put(anio, new EntradaCacheMep(resultado, Instant.now().plus(DURACION_CACHE_MEP)));
            return resultado;
        } catch (RestClientException e) {
            log.warn("No se pudo obtener el calendario del MEP para el año {}: {}", anio, e.getMessage());
            // Si hay una respuesta anterior (aunque ya venció), es mejor mostrar eso que nada.
            return entrada != null ? entrada.eventos() : List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<EventoMepDTO> buscarEventos(Long institucionId, Integer mes, String categoria, String texto) {
        List<EventoMepDTO> base = new ArrayList<>(listarEventosDelAnio(anioActual()));
        base.addAll(listarPropias(institucionId));

        LocalDate inicioMes = mes != null ? YearMonth.of(anioActual(), mes).atDay(1) : null;
        LocalDate finMes = mes != null ? YearMonth.of(anioActual(), mes).atEndOfMonth() : null;
        String textoNormalizado = texto != null && !texto.isBlank() ? texto.trim().toLowerCase() : null;
        String categoriaNormalizada = categoria != null && !categoria.isBlank() ? categoria : null;

        return base.stream()
                .filter(e -> mes == null || (e.getFechaInicio() != null && e.getFechaFin() != null
                        && !e.getFechaFin().isBefore(inicioMes) && !e.getFechaInicio().isAfter(finMes)))
                .filter(e -> categoriaNormalizada == null || categoriaNormalizada.equals(e.getNombreCategoria()))
                .filter(e -> textoNormalizado == null
                        || (e.getTitulo() != null && e.getTitulo().toLowerCase().contains(textoNormalizado))
                        || (e.getDescripcion() != null && e.getDescripcion().toLowerCase().contains(textoNormalizado)))
                .sorted(Comparator.comparing(EventoMepDTO::getFechaInicio,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarCategorias(Long institucionId) {
        List<String> categorias = new ArrayList<>();
        listarEventosDelAnio(anioActual()).forEach(e -> categorias.add(e.getNombreCategoria()));
        listarPropias(institucionId).forEach(e -> categorias.add(e.getNombreCategoria()));
        return categorias.stream()
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EventoMepDTO> obtenerEvento(Long institucionId, String id) {
        if (EventoMepDTO.esIdPropia(id)) {
            return actividadPropiaRepository.findByIdAndInstitucionId(EventoMepDTO.idNumericoPropia(id), institucionId)
                    .map(EventoMepDTO::deActividadPropia);
        }
        return listarEventosDelAnio(anioActual()).stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    /** Eventos cuyo rango de fechas se solapa con [desde, hasta], sin importar el año. */
    @Transactional(readOnly = true)
    public List<EventoMepDTO> listarEventosEnRango(Long institucionId, LocalDate desde, LocalDate hasta) {
        List<EventoMepDTO> eventos = new ArrayList<>();
        for (int anio = desde.getYear(); anio <= hasta.getYear(); anio++) {
            eventos.addAll(listarEventosDelAnio(anio));
        }
        eventos.addAll(listarPropias(institucionId));
        return eventos.stream()
                .filter(e -> e.getFechaInicio() != null && e.getFechaFin() != null)
                .filter(e -> !e.getFechaFin().isBefore(desde) && !e.getFechaInicio().isAfter(hasta))
                .sorted(Comparator.comparing(EventoMepDTO::getFechaInicio))
                .toList();
    }

    private List<EventoMepDTO> listarPropias(Long institucionId) {
        return actividadPropiaRepository.findByInstitucionIdOrderByFechaInicioAsc(institucionId).stream()
                .map(EventoMepDTO::deActividadPropia)
                .toList();
    }

    // ─── CRUD de actividades propias (creadas por Director/Admin) ───────

    public ActividadInstitucionalPropia guardarActividadPropia(Long institucionId, Long usuarioId, Long id,
            String titulo, String descripcion, LocalDate fechaInicio, LocalDate fechaFin,
            String categoria, String enlace) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        LocalDate finReal = (fechaFin == null || fechaFin.isBefore(fechaInicio)) ? fechaInicio : fechaFin;

        ActividadInstitucionalPropia actividad = id == null
                ? new ActividadInstitucionalPropia()
                : obtenerActividadPropiaEntidad(institucionId, id);
        if (actividad.getId() == null) {
            actividad.setInstitucion(obtenerInstitucion(institucionId));
            actividad.setCreadoPor(obtenerUsuario(usuarioId));
        }
        actividad.setTitulo(titulo.trim());
        actividad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
        actividad.setFechaInicio(fechaInicio);
        actividad.setFechaFin(finReal);
        actividad.setCategoria(categoria != null && !categoria.isBlank() ? categoria.trim() : null);
        actividad.setEnlace(enlace != null && !enlace.isBlank() ? enlace.trim() : null);
        ActividadInstitucionalPropia guardada = actividadPropiaRepository.save(actividad);
        log.info("Actividad institucional propia guardada: id={}, titulo={}", guardada.getId(), guardada.getTitulo());
        return guardada;
    }

    public void eliminarActividadPropia(Long institucionId, Long id) {
        ActividadInstitucionalPropia actividad = obtenerActividadPropiaEntidad(institucionId, id);
        actividadPropiaRepository.delete(actividad);
        log.info("Actividad institucional propia eliminada: id={}", id);
    }

    public ActividadInstitucionalPropia obtenerActividadPropiaEntidad(Long institucionId, Long id) {
        return actividadPropiaRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad institucional no encontrada"));
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Institucion obtenerInstitucion(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }

    private int anioActual() {
        return LocalDate.now().getYear();
    }
}
