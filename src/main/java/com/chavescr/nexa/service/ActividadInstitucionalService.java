package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chavescr.nexa.dto.EventoMepDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Consume el calendario oficial de actividades del MEP
 * (https://calendario.mep.go.cr/{anio}/webservices/obtener_eventos.php)
 * para llenar la sección "Actividades Institucionales" de la Agenda.
 */
@Service
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

    private final RestClient restClient;

    public ActividadInstitucionalService() {
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
        try {
            List<EventoMepDTO> eventos = restClient.get()
                    .uri(URL_EVENTOS.formatted(anio))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EventoMepDTO>>() { });
            return eventos != null ? eventos : List.of();
        } catch (RestClientException e) {
            log.warn("No se pudo obtener el calendario del MEP para el año {}: {}", anio, e.getMessage());
            return List.of();
        }
    }

    public List<EventoMepDTO> buscarEventos(Integer mes, String categoria, String texto) {
        List<EventoMepDTO> base = listarEventosDelAnio(anioActual());

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

    public List<String> listarCategorias() {
        return listarEventosDelAnio(anioActual()).stream()
                .map(EventoMepDTO::getNombreCategoria)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public Optional<EventoMepDTO> obtenerEvento(String id) {
        return listarEventosDelAnio(anioActual()).stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    private int anioActual() {
        return LocalDate.now().getYear();
    }
}
