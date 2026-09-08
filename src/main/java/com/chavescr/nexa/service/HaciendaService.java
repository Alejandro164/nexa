package com.chavescr.nexa.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chavescr.nexa.dto.HaciendaContribuyenteDTO;

/**
 * Consulta la API pública de Situación Tributaria del Ministerio de Hacienda
 * (https://api.hacienda.go.cr/fe/ae?identificacion={cedula}) para autocompletar el nombre de un
 * visitante que no está registrado como padre ni como visitante recurrente en la aplicación.
 */
@Service
public class HaciendaService {

    private static final Logger log = LoggerFactory.getLogger(HaciendaService.class);
    private static final String URL_CONSULTA = "https://api.hacienda.go.cr/fe/ae?identificacion=%s";

    private final RestClient restClient;

    public HaciendaService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(4000);
        requestFactory.setReadTimeout(6000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /** Vacío si la identificación no tiene un formato consultable, si Hacienda no la conoce, o si la API falla. */
    public Optional<HaciendaContribuyenteDTO> consultar(String identificacion) {
        if (identificacion == null) {
            return Optional.empty();
        }
        String soloDigitos = identificacion.replaceAll("[^0-9]", "");
        if (soloDigitos.length() < 9 || soloDigitos.length() > 12) {
            return Optional.empty();
        }
        try {
            HaciendaContribuyenteDTO resultado = restClient.get()
                    .uri(URL_CONSULTA.formatted(soloDigitos))
                    .retrieve()
                    .body(HaciendaContribuyenteDTO.class);
            return Optional.ofNullable(resultado)
                    .filter(r -> r.getNombre() != null && !r.getNombre().isBlank());
        } catch (RestClientException e) {
            log.warn("No se pudo consultar la API de Hacienda para identificación {}: {}", soloDigitos, e.getMessage());
            return Optional.empty();
        }
    }
}
