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

import com.chavescr.nexa.dto.ComunicadosVista;
import com.chavescr.nexa.dto.MensajeriaVista;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.ComunicadoService;
import com.chavescr.nexa.service.MensajeriaService;

import jakarta.servlet.http.HttpServletRequest;
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

    private static final String COMUNICADOS = "comunicacion/comunicados/comunicados";

    private final MensajeriaService mensajeriaService;
    private final ComunicadoService comunicadoService;

    public ComunicacionController(MensajeriaService mensajeriaService, ComunicadoService comunicadoService) {
        this.mensajeriaService = mensajeriaService;
        this.comunicadoService = comunicadoService;
    }

    @GetMapping
    public String comunicacion(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @RequestParam(name = "c", required = false) Long conversacionId,
            @RequestParam(name = "tab", required = false) String pestana,
            @RequestParam(name = "o", required = false) Long oficioId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, HttpServletRequest request,
            Model model) {
        Long direccionId = direccionId(session);
        boolean comunicados = "comunicados".equals(pestana);
        model.addAttribute("pestana", comunicados ? "comunicados" : "mensajeria");
        if (comunicados) {
            cargarComunicados(model, usuario.getId(), direccionId, supervisa(request), null, oficioId);
        } else {
            cargarContenido(model, usuario.getId(), direccionId, conversacionId);
        }
        return htmxRequest ? "comunicacion/index :: htmx-content" : "comunicacion/index";
    }

    @GetMapping("/mensajeria")
    public String mensajeria(@RequestParam(name = "c", required = false) Long conversacionId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        cargarContenido(model, usuario.getId(), direccionId(session), conversacionId);
        return VISTA + " :: content";
    }

    // ── Comunicados ───────────────────────────────────────────────────────────

    @GetMapping("/comunicados")
    public String comunicados(@RequestParam(name = "d", required = false) String clave,
            @RequestParam(name = "o", required = false) Long oficioId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, HttpServletRequest request,
            Model model) {
        cargarComunicados(model, usuario.getId(), direccionId(session), supervisa(request), clave, oficioId);
        return COMUNICADOS + " :: content";
    }

    /** La lista de destinatarios (con o sin búsqueda). Solo administración y dirección. */
    @GetMapping("/comunicados/destinatarios")
    public String destinatarios(@RequestParam(name = "q", required = false) String busqueda,
            @RequestParam(name = "d", required = false) String seleccionada,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, HttpServletRequest request,
            HttpServletResponse response, Model model) {
        if (!supervisa(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return COMUNICADOS + " :: vacio";
        }
        model.addAttribute("destinatarios", comunicadoService.destinatarios(direccionId(session), busqueda));
        model.addAttribute("seleccionada", seleccionada);
        return COMUNICADOS + " :: lista";
    }

    /** Los oficios del destinatario elegido (panel completo: cabecera, filtros y lista). */
    @GetMapping("/comunicados/panel")
    public String panel(@RequestParam(name = "d") String clave, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            model.addAttribute("panel", comunicadoService.panel(clave, direccionId(session), usuario.getId(),
                    supervisa(request), null, null, null));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return COMUNICADOS + " :: vacio";
        }
        model.addAttribute("resaltarId", null);
        return COMUNICADOS + " :: panel";
    }

    /** Solo la lista de oficios, al cambiar la búsqueda, el tipo o el año. */
    @GetMapping("/comunicados/oficios")
    public String oficios(@RequestParam(name = "d") String clave,
            @RequestParam(name = "q", required = false) String busqueda,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "anio", required = false) Integer anio,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, HttpServletRequest request,
            HttpServletResponse response, Model model) {
        try {
            model.addAttribute("panel", comunicadoService.panel(clave, direccionId(session), usuario.getId(),
                    supervisa(request), busqueda, tipo, anio));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return COMUNICADOS + " :: vacio";
        }
        model.addAttribute("resaltarId", null);
        return COMUNICADOS + " :: oficios-lista";
    }

    /** El documento del oficio; abrirlo o descargarlo deja constancia de lectura. */
    @GetMapping("/comunicados/oficio/{id}/documento")
    public ResponseEntity<Resource> documento(@PathVariable Long id,
            @RequestParam(name = "descargar", defaultValue = "false") boolean descargar,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, HttpServletRequest request) {
        ComunicadosVista.Documento documento;
        try {
            documento = comunicadoService.documento(id, direccionId(session), usuario.getId(), supervisa(request),
                    descargar);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.exists(documento.ruta())) {
            return ResponseEntity.notFound().build();
        }
        ContentDisposition disposicion = (documento.enLinea() ? ContentDisposition.inline()
                : ContentDisposition.attachment()).filename(documento.nombre(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(documento.ruta()));
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

    /**
     * Administración y dirección supervisan: eligen un destinatario y ven el progreso de lectura. Quien no
     * supervisa ve solo los oficios que le llegan.
     */
    private void cargarComunicados(Model model, Long usuarioId, Long direccionId, boolean supervision, String clave,
            Long resaltarId) {
        model.addAttribute("soloPropios", !supervision);
        model.addAttribute("resaltarId", resaltarId);
        List<ComunicadosVista.Destinatario> destinatarios = supervision
                ? comunicadoService.destinatarios(direccionId, null)
                : List.of();
        model.addAttribute("destinatarios", destinatarios);

        String elegida;
        if (!supervision || resaltarId != null) {
            // Desde un aviso, o sin permiso de supervisión: los oficios de la propia persona.
            elegida = "PERSONA-" + usuarioId;
        } else if (clave != null && !clave.isBlank()) {
            elegida = clave;
        } else {
            elegida = destinatarios.isEmpty() ? null : destinatarios.get(0).clave();
        }
        ComunicadosVista.Panel panel = null;
        if (elegida != null) {
            try {
                panel = comunicadoService.panel(elegida, direccionId, usuarioId, supervision, null, null, null);
            } catch (IllegalArgumentException e) {
                // Destinatario que ya no existe: se muestra el panel vacío.
            }
        }
        model.addAttribute("panel", panel);
        model.addAttribute("seleccionada", panel == null ? null : panel.clave());
    }

    private static boolean supervisa(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
    }


    private Long direccionId(HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }
}
