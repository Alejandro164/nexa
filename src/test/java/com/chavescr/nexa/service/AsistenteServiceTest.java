package com.chavescr.nexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.chavescr.nexa.dto.ContextoAsistente;
import com.chavescr.nexa.dto.MensajeAsistente;

class AsistenteServiceTest {

    @Test
    void elHistorialAlternaTurnosYDejaFueraLosErrores() {
        List<MensajeAsistente> historial = List.of(
                MensajeAsistente.delUsuario("¿Qué tengo hoy?"),
                MensajeAsistente.delAsistente("Tienes 3 lecciones."),
                MensajeAsistente.delUsuario("¿Y mañana?"),
                MensajeAsistente.deError("El asistente tuvo un problema."),
                MensajeAsistente.delUsuario("¿Y mañana? (otra vez)"),
                MensajeAsistente.delAsistente("Mañana tienes 2."));

        List<Map<String, Object>> mensajes = AsistenteService.mensajesDe(historial);

        assertEquals(List.of("user", "assistant", "user", "assistant"),
                mensajes.stream().map(m -> m.get("role")).toList());
        assertEquals("¿Y mañana? (otra vez)", mensajes.get(2).get("content"),
                "La pregunta que falló se reemplaza por la reintentada");
    }

    @Test
    void elHistorialTerminaEnUnaRespuestaParaQueLaPreguntaNuevaAlterne() {
        List<MensajeAsistente> historial = List.of(
                MensajeAsistente.delUsuario("Hola"),
                MensajeAsistente.delAsistente("Hola, ¿en qué te ayudo?"),
                MensajeAsistente.delUsuario("Pregunta sin respuesta"),
                MensajeAsistente.deError("Falló"));

        List<Map<String, Object>> mensajes = AsistenteService.mensajesDe(historial);

        assertEquals(2, mensajes.size());
        assertEquals("assistant", mensajes.get(1).get("role"));
    }

    @Test
    void elHistorialSeRecortaALosUltimosMensajes() {
        List<MensajeAsistente> historial = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            historial.add(MensajeAsistente.delUsuario("Pregunta " + i));
            historial.add(MensajeAsistente.delAsistente("Respuesta " + i));
        }

        List<Map<String, Object>> mensajes = AsistenteService.mensajesDe(historial);

        assertEquals(AsistenteService.MAX_HISTORIAL, mensajes.size());
        assertEquals("user", mensajes.get(0).get("role"));
        assertEquals("Respuesta 29", mensajes.get(mensajes.size() - 1).get("content"));
    }

    @Test
    void elContextoNombraAlUsuarioSuRolYLaDireccion() {
        String contexto = AsistenteService.contexto(new ContextoAsistente(1L, "Liceo Alpha", 2L, "María González",
                Set.of("ROLE_DIRECTOR", "ROLE_DOCENTE")));

        assertTrue(contexto.contains("María González"));
        assertTrue(contexto.contains("director, docente"));
        assertTrue(contexto.contains("Liceo Alpha"));
        assertTrue(contexto.contains("hora de Costa Rica"));
    }

    @Test
    void losErroresDeLaApiSeExplicanAlUsuario() {
        assertTrue(AsistenteService.mensajeDeError(new AsistenteService.ErrorApi(401, "")).contains("API key no es válida"));
        assertTrue(AsistenteService.mensajeDeError(new AsistenteService.ErrorApi(402, "credits")).contains("sin créditos"));
        assertTrue(AsistenteService.mensajeDeError(new AsistenteService.ErrorApi(429, "")).contains("muchas consultas"));
        assertTrue(AsistenteService.mensajeDeError(new AsistenteService.ErrorApi(503, "")).contains("tuvo un problema"));
    }
}
