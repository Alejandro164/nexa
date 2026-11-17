package com.chavescr.nexa.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.ContextoAsistente;
import com.chavescr.nexa.dto.MensajeAsistente;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.AsistenteConfiguracionService;
import com.chavescr.nexa.service.AsistenteService;

import jakarta.servlet.http.HttpSession;

/**
 * Chat Asistente (botón flotante de todas las páginas). La conversación vive solo en la sesión, una por
 * dirección, y se borra al cerrar sesión.
 */
@Controller
@RequestMapping("/asistente")
public class AsistenteController {

    static final int MAX_PREGUNTA = 2000;
    private static final String SESSION_HISTORIAL = "SESSION_ASISTENTE_HISTORIAL_";
    private static final String SESSION_CONVERSACION = "SESSION_ASISTENTE_CONVERSACION_";

    @Autowired
    private AsistenteService asistenteService;

    @Autowired
    private AsistenteConfiguracionService configuracionService;

    @GetMapping("/conversacion")
    public String conversacion(Model model, HttpSession session) {
        cargar(model, session);
        return "asistente/widget :: conversacion";
    }

    @PostMapping("/mensaje")
    public String mensaje(@RequestParam String pregunta, @AuthenticationPrincipal CustomUserDetails usuario,
            Model model, HttpSession session) {
        String texto = pregunta == null ? "" : pregunta.trim();
        Long direccionId = direccionActual(session);
        if (!texto.isEmpty() && direccionId != null && configuracionService.estaDisponible()) {
            List<MensajeAsistente> historial = historial(session, direccionId);
            if (texto.length() > MAX_PREGUNTA) {
                texto = texto.substring(0, MAX_PREGUNTA);
            }
            List<MensajeAsistente> anteriores = List.copyOf(historial);
            historial.add(MensajeAsistente.delUsuario(texto));
            try {
                historial.add(MensajeAsistente.delAsistente(
                        asistenteService.responder(contexto(usuario, session, direccionId), anteriores, texto,
                                conversacionId(session, direccionId))));
            } catch (AsistenteService.AsistenteException e) {
                historial.add(MensajeAsistente.deError(e.getMessage()));
            }
            guardar(session, direccionId, historial);
        }
        cargar(model, session);
        return "asistente/widget :: conversacion";
    }

    @PostMapping("/nueva")
    public String nueva(Model model, HttpSession session) {
        Long direccionId = direccionActual(session);
        if (direccionId != null) {
            session.removeAttribute(SESSION_HISTORIAL + direccionId);
            session.removeAttribute(SESSION_CONVERSACION + direccionId);
        }
        cargar(model, session);
        return "asistente/widget :: conversacion";
    }

    private void cargar(Model model, HttpSession session) {
        Long direccionId = direccionActual(session);
        model.addAttribute("asistenteSinDireccion", direccionId == null);
        model.addAttribute("asistenteDisponible", configuracionService.estaDisponible());
        model.addAttribute("mensajesAsistente", direccionId == null ? List.of() : historial(session, direccionId));
    }

    private static ContextoAsistente contexto(CustomUserDetails usuario, HttpSession session, Long direccionId) {
        return new ContextoAsistente(direccionId, (String) session.getAttribute("SESSION_DIRECCION_NOMBRE"),
                usuario.getId(), usuario.getNombre(), usuario.getRoles());
    }

    @SuppressWarnings("unchecked")
    private static List<MensajeAsistente> historial(HttpSession session, Long direccionId) {
        Object guardado = session.getAttribute(SESSION_HISTORIAL + direccionId);
        return guardado instanceof List<?> lista ? new ArrayList<>((List<MensajeAsistente>) lista) : new ArrayList<>();
    }

    /** Se vuelve a poner el atributo para que la sesión en Redis (prod) guarde el cambio. */
    private static void guardar(HttpSession session, Long direccionId, List<MensajeAsistente> historial) {
        session.setAttribute(SESSION_HISTORIAL + direccionId, new ArrayList<>(historial));
    }

    /** Id de la conversación, para que xAI reutilice la caché del prompt entre preguntas. */
    private static String conversacionId(HttpSession session, Long direccionId) {
        Object guardado = session.getAttribute(SESSION_CONVERSACION + direccionId);
        if (guardado instanceof String id) {
            return id;
        }
        String id = java.util.UUID.randomUUID().toString();
        session.setAttribute(SESSION_CONVERSACION + direccionId, id);
        return id;
    }

    private static Long direccionActual(HttpSession session) {
        Object actual = session.getAttribute("SESSION_DIRECCION_ID");
        return actual instanceof Long id ? id : null;
    }
}
