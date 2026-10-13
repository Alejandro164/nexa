package com.chavescr.nexa.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.dto.MensajeriaVista;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.MensajeriaService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Canal de comunicación. La mensajería se actualiza por fragmentos con htmx: abrir una conversación,
 * enviar, reaccionar y el sondeo del hilo devuelven fragmentos de {@code mensajeria.html}
 * (no hay redirect porque el navegador no recarga la página).
 */
@Controller
@RequestMapping("/comunicacion")
public class ComunicacionController {

    private static final String VISTA = "comunicacion/mensajeria/mensajeria";
    private static final String FRAGMENTO_VACIO = VISTA + " :: vacio";

    private final MensajeriaService mensajeriaService;

    public ComunicacionController(MensajeriaService mensajeriaService) {
        this.mensajeriaService = mensajeriaService;
    }

    @GetMapping
    public String comunicacion(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @RequestParam(name = "c", required = false) Long conversacionId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        cargarContenido(model, usuario.getId(), direccionId(session), conversacionId);
        return htmxRequest ? "comunicacion/index :: htmx-content" : "comunicacion/index";
    }

    @GetMapping("/mensajeria")
    public String mensajeria(@RequestParam(name = "c", required = false) Long conversacionId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        cargarContenido(model, usuario.getId(), direccionId(session), conversacionId);
        return VISTA + " :: content";
    }

    @GetMapping("/comunicados")
    public String comunicados() {
        return "comunicacion/comunicados/comunicados :: content";
    }

    /** Abre una conversación en el panel derecho y refresca la lista (el contador de no leídos baja a 0). */
    @GetMapping("/mensajeria/{id}")
    public String abrir(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, HttpServletResponse response, Model model) {
        Long direccionId = direccionId(session);
        try {
            cargarHilo(model, id, usuario.getId(), direccionId);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return FRAGMENTO_VACIO;
        }
        return VISTA + " :: hilo-respuesta";
    }

    /** Repinta la lista (contadores y vistas previas) sin tocar el hilo abierto. */
    @GetMapping("/mensajeria/lista")
    public String lista(@RequestParam(name = "c", required = false) Long conversacionId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        model.addAttribute("conversaciones", mensajeriaService.listar(usuario.getId(), direccionId(session)));
        model.addAttribute("seleccionadaId", conversacionId);
        return VISTA + " :: lista(oob=false)";
    }

    /** Sondeo: 204 (sin cuerpo, htmx no repinta) mientras no haya mensajes, reacciones o lecturas nuevas. */
    @GetMapping("/mensajeria/{id}/mensajes")
    public String sondear(@PathVariable Long id, @RequestParam(required = false) String firma,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session,
            HttpServletResponse response, Model model) {
        Long direccionId = direccionId(session);
        try {
            Optional<MensajeriaVista.Hilo> hilo = mensajeriaService.refrescar(id, usuario.getId(), direccionId,
                    firma);
            if (hilo.isEmpty()) {
                response.setStatus(HttpStatus.NO_CONTENT.value());
                return FRAGMENTO_VACIO;
            }
            model.addAttribute("hilo", hilo.get());
            model.addAttribute("conversaciones", mensajeriaService.listar(usuario.getId(), direccionId));
            model.addAttribute("seleccionadaId", id);
            model.addAttribute("reaccionesRapidas", MensajeriaService.REACCIONES_RAPIDAS);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return FRAGMENTO_VACIO;
        }
        return VISTA + " :: mensajes";
    }

    @PostMapping("/mensajeria/{id}/enviar")
    public String enviar(@PathVariable Long id, @RequestParam(required = false) String texto,
            @RequestParam(name = "respuestaA", required = false) Long respuestaId,
            @RequestParam(name = "archivos", required = false) List<MultipartFile> archivos,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session,
            HttpServletResponse response, Model model) {
        Long direccionId = direccionId(session);
        try {
            mensajeriaService.enviar(id, usuario.getId(), texto, respuestaId, archivos);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("error", "No se pudo guardar el archivo. Intenta de nuevo.");
        }
        try {
            cargarHilo(model, id, usuario.getId(), direccionId);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return FRAGMENTO_VACIO;
        }
        return VISTA + " :: mensajes";
    }

    @PostMapping("/mensajeria/mensaje/{mensajeId}/reaccion")
    public String reaccionar(@PathVariable Long mensajeId, @RequestParam String emoji,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session,
            HttpServletResponse response, Model model) {
        Long direccionId = direccionId(session);
        try {
            Long conversacionId = mensajeriaService.reaccionar(mensajeId, emoji, usuario.getId());
            cargarHilo(model, conversacionId, usuario.getId(), direccionId);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return FRAGMENTO_VACIO;
        }
        return VISTA + " :: mensajes";
    }

    @GetMapping("/mensajeria/archivo/{mensajeId}")
    public ResponseEntity<Resource> archivo(@PathVariable Long mensajeId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session) throws IOException {
        MensajeriaVista.ArchivoDescarga archivo;
        try {
            archivo = mensajeriaService.archivo(mensajeId, usuario.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.exists(archivo.ruta())) {
            return ResponseEntity.notFound().build();
        }
        // Solo las fotos se muestran en la página; todo lo demás se descarga.
        ContentDisposition disposicion = (archivo.imagen() ? ContentDisposition.inline()
                : ContentDisposition.attachment()).filename(archivo.nombre(), StandardCharsets.UTF_8).build();
        MediaType tipo = archivo.tipoContenido() == null ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(archivo.tipoContenido());
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(archivo.ruta()));
    }

    @GetMapping("/mensajeria/contactos")
    public String contactos(@RequestParam(name = "q", required = false) String busqueda,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        model.addAttribute("contactos", mensajeriaService.contactos(usuario.getId(), direccionId(session), busqueda));
        return VISTA + " :: contactos";
    }

    /** Empieza (o retoma) una conversación con la persona elegida y devuelve la vista completa con ella abierta. */
    @PostMapping("/mensajeria/nueva")
    public String nueva(@RequestParam Long usuarioId, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, Model model) {
        Long direccionId = direccionId(session);
        Long conversacionId = null;
        try {
            conversacionId = mensajeriaService.abrirOCrear(usuario.getId(), usuarioId, direccionId);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarContenido(model, usuario.getId(), direccionId, conversacionId);
        return VISTA + " :: content";
    }

    // ── Apoyo ─────────────────────────────────────────────────────────────────

    private void cargarContenido(Model model, Long usuarioId, Long direccionId, Long conversacionId) {
        model.addAttribute("conversaciones", mensajeriaService.listar(usuarioId, direccionId));
        model.addAttribute("reaccionesRapidas", MensajeriaService.REACCIONES_RAPIDAS);
        model.addAttribute("hilo", null);
        model.addAttribute("seleccionadaId", null);
        if (conversacionId != null) {
            try {
                cargarHilo(model, conversacionId, usuarioId, direccionId);
            } catch (IllegalArgumentException e) {
                // Enlace viejo o conversación ajena: se muestra la lista sin nada abierto.
            }
        }
    }

    private void cargarHilo(Model model, Long conversacionId, Long usuarioId, Long direccionId) {
        MensajeriaVista.Hilo hilo = mensajeriaService.abrir(conversacionId, usuarioId, direccionId);
        model.addAttribute("hilo", hilo);
        model.addAttribute("seleccionadaId", hilo.id());
        model.addAttribute("conversaciones", mensajeriaService.listar(usuarioId, direccionId));
        model.addAttribute("reaccionesRapidas", MensajeriaService.REACCIONES_RAPIDAS);
    }

    private Long direccionId(HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }
}
