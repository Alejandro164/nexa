package com.chavescr.nexa.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.exception.WhatsAppApiException;
import com.chavescr.nexa.service.WhatsAppConfiguracionService.CredencialesWhatsApp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Envío de mensajes salientes a través de WhatsApp Business Cloud API (Meta Graph API). */
@Service
public class WhatsAppMensajeService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMensajeService.class);
    private static final String GRAPH_BASE_URL = "https://graph.facebook.com/v21.0/";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    /** Plantilla de ejemplo que Meta aprueba automáticamente en toda cuenta: sirve para probar la conexión. */
    private static final String PLANTILLA_PRUEBA = "hello_world";
    private static final String IDIOMA_PLANTILLA_PRUEBA = "en_US";

    public record InfoConexion(String numeroVerificado, String nombreVerificado) {
    }

    private final WhatsAppConfiguracionService configuracionService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WhatsAppMensajeService(WhatsAppConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Valida las credenciales guardadas contra la Graph API, sin enviar ningún mensaje. */
    public InfoConexion probarConexion(Long institucionId) {
        CredencialesWhatsApp creds = configuracionService.obtenerCredenciales(institucionId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_BASE_URL + creds.phoneNumberId()
                        + "?fields=verified_name,display_phone_number"))
                .header("Authorization", "Bearer " + creds.accessToken())
                .timeout(TIMEOUT)
                .GET()
                .build();

        JsonNode cuerpo = ejecutar(request);
        String numero = textoDe(cuerpo, "display_phone_number");
        String nombre = textoDe(cuerpo, "verified_name");
        configuracionService.registrarPruebaExitosa(institucionId, numero);
        log.info("Conexión de WhatsApp verificada: institucionId={}, numero={}", institucionId, numero);
        return new InfoConexion(numero, nombre);
    }

    /** Envía la plantilla de ejemplo "hello_world" a un número, para comprobar el envío de extremo a extremo. */
    public void enviarMensajePrueba(Long institucionId, String telefonoDestino) {
        CredencialesWhatsApp creds = configuracionService.obtenerCredenciales(institucionId);
        enviarPlantilla(creds, telefonoDestino, PLANTILLA_PRUEBA, IDIOMA_PLANTILLA_PRUEBA);
        configuracionService.registrarPruebaExitosa(institucionId, null);
        log.info("Mensaje de prueba de WhatsApp enviado: institucionId={}, destino={}", institucionId,
                telefonoDestino);
    }

    /**
     * Envía una plantilla ya aprobada en Meta Business Manager. Pensado para que otras funcionalidades
     * (ausencias, notas, etc.) lo reutilicen una vez definan sus propias plantillas.
     */
    public void enviarNotificacionPlantilla(Long institucionId, String telefonoDestino, String nombrePlantilla,
            String codigoIdioma) {
        if (!configuracionService.estaActivo(institucionId)) {
            throw new IllegalStateException("El envío de notificaciones por WhatsApp está desactivado para esta institución");
        }
        CredencialesWhatsApp creds = configuracionService.obtenerCredenciales(institucionId);
        enviarPlantilla(creds, telefonoDestino, nombrePlantilla, codigoIdioma);
    }

    private void enviarPlantilla(CredencialesWhatsApp creds, String telefonoDestino, String nombrePlantilla,
            String codigoIdioma) {
        String telefono = normalizarTelefono(telefonoDestino);
        if (telefono.isBlank()) {
            throw new IllegalArgumentException("Indica el número de destino, con código de país");
        }

        Map<String, Object> plantilla = new LinkedHashMap<>();
        plantilla.put("name", nombrePlantilla);
        plantilla.put("language", Map.of("code", codigoIdioma));

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("messaging_product", "whatsapp");
        cuerpo.put("to", telefono);
        cuerpo.put("type", "template");
        cuerpo.put("template", plantilla);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_BASE_URL + creds.phoneNumberId() + "/messages"))
                .header("Authorization", "Bearer " + creds.accessToken())
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(escribirJson(cuerpo)))
                .build();

        ejecutar(request);
    }

    private JsonNode ejecutar(HttpRequest request) {
        HttpResponse<String> respuesta;
        try {
            respuesta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new WhatsAppApiException("No se pudo contactar a WhatsApp: " + e.getMessage(), e);
        }

        JsonNode json = leerJson(respuesta.body());
        if (respuesta.statusCode() >= 200 && respuesta.statusCode() < 300) {
            return json;
        }
        throw new WhatsAppApiException(mensajeDeError(json, respuesta.statusCode()));
    }

    private JsonNode leerJson(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(cuerpo);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return objectMapper.nullNode();
        }
    }

    private String mensajeDeError(JsonNode json, int statusCode) {
        if (json != null) {
            String mensaje = textoDe(json.path("error"), "message");
            if (mensaje != null && !mensaje.isBlank()) {
                return "WhatsApp rechazó la solicitud: " + mensaje;
            }
        }
        return "WhatsApp respondió con un error (HTTP " + statusCode + ")";
    }

    private String textoDe(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private String escribirJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("No se pudo construir la solicitud a WhatsApp", e);
        }
    }

    private String normalizarTelefono(String telefono) {
        return telefono == null ? "" : telefono.replaceAll("[^0-9]", "");
    }
}
