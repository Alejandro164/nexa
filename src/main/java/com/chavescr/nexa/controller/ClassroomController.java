package com.chavescr.nexa.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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

import com.chavescr.nexa.dto.ContextoClase;
import com.chavescr.nexa.entity.ClassroomAdjunto;
import com.chavescr.nexa.entity.ClassroomPublicacion;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.TipoPublicacion;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.ClassroomService;
import com.chavescr.nexa.service.ClassroomService.DatosPublicacion;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/classroom")
public class ClassroomController {

    private static final String FRAGMENTO_TABLON = "classroom/clase/tablon/tablon :: content";
    private static final String FRAGMENTO_COMENTARIOS = "classroom/clase/tablon/tablon :: comentarios";
    private static final String FRAGMENTO_TAREAS = "classroom/clase/tareas/tareas :: content";
    private static final String FRAGMENTO_DETALLE = "classroom/clase/tareas/detalle :: content";
    private static final String FRAGMENTO_MATERIALES = "classroom/clase/materiales/materiales :: content";
    private static final String FRAGMENTO_PERSONAS = "classroom/clase/personas/personas :: content";
    private static final String FRAGMENTO_FORM = "classroom/clase/formulario :: form-content";

    // Rubros de evaluación a los que puede ir una tarea calificable (mismos nombres que Gestión Académica)
    private static final Map<ClaveComponente, String> RUBROS = new LinkedHashMap<>();
    static {
        RUBROS.put(ClaveComponente.TAREA, "Tarea");
        RUBROS.put(ClaveComponente.PROYECTO, "Proyecto");
        RUBROS.put(ClaveComponente.EXAMEN, "Prueba");
        RUBROS.put(ClaveComponente.COTIDIANO, "Trabajo cotidiano");
    }

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    // ── Páginas ───────────────────────────────────────────────────────────────

    @GetMapping
    public String index(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        model.addAttribute("tarjetas",
                classroomService.listarClases(requerirDireccion(session), usuario.getId(), usuario.getRoles()));
        return htmxRequest ? "classroom/index :: htmx-content" : "classroom/index";
    }

    @GetMapping("/clase/{claseId}")
    public String clase(@PathVariable Long claseId,
            @RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        model.addAttribute("ctx", contexto(claseId, usuario, session));
        return htmxRequest ? "classroom/clase/index :: htmx-content" : "classroom/clase/index";
    }

    // ── Pestañas ──────────────────────────────────────────────────────────────

    @GetMapping("/clase/{claseId}/tablon")
    public String tablon(@PathVariable Long claseId, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, Model model) {
        return cargarTablon(contexto(claseId, usuario, session), model);
    }

    @GetMapping("/clase/{claseId}/tareas")
    public String tareas(@PathVariable Long claseId, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, Model model) {
        return cargarTareas(contexto(claseId, usuario, session), model);
    }

    @GetMapping("/clase/{claseId}/materiales")
    public String materiales(@PathVariable Long claseId, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, Model model) {
        return cargarMateriales(contexto(claseId, usuario, session), model);
    }

    @GetMapping("/clase/{claseId}/personas")
    public String personas(@PathVariable Long claseId, @AuthenticationPrincipal CustomUserDetails usuario,
            HttpSession session, Model model) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        model.addAttribute("ctx", ctx);
        model.addAttribute("estudiantes", classroomService.estudiantes(ctx));
        return FRAGMENTO_PERSONAS;
    }

    // ── Publicaciones ─────────────────────────────────────────────────────────

    @GetMapping("/clase/{claseId}/form")
    public String formulario(@PathVariable Long claseId, @RequestParam(required = false) TipoPublicacion tipo,
            @RequestParam(required = false) Long publicacionId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        ClassroomPublicacion pub = publicacionId != null ? classroomService.publicacion(ctx, publicacionId) : null;
        model.addAttribute("ctx", ctx);
        model.addAttribute("pub", pub);
        model.addAttribute("tipo", pub != null ? pub.getTipo() : (tipo != null ? tipo : TipoPublicacion.ANUNCIO));
        model.addAttribute("temas", classroomService.temas(ctx));
        model.addAttribute("rubros", RUBROS);
        model.addAttribute("porcentaje", pub != null ? classroomService.porcentajeComponente(ctx, pub) : null);
        return FRAGMENTO_FORM;
    }

    @PostMapping("/clase/{claseId}/publicacion")
    public String guardarPublicacion(@PathVariable Long claseId,
            @RequestParam(required = false) Long publicacionId,
            @RequestParam(required = false) TipoPublicacion tipo,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String contenido,
            @RequestParam(required = false) String tema,
            @RequestParam(required = false) String fechaEntrega,
            @RequestParam(required = false) Integer puntosTotales,
            @RequestParam(required = false) ClaveComponente claveComponente,
            @RequestParam(required = false) Integer porcentaje,
            @RequestParam(required = false) List<MultipartFile> archivos,
            @RequestParam(required = false) String enlaces,
            @RequestParam(required = false) List<Long> quitarAdjunto,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        ClassroomPublicacion guardada;
        try {
            guardada = classroomService.guardarPublicacion(ctx, publicacionId, tipo,
                    new DatosPublicacion(titulo, contenido, tema, parseFechaHora(fechaEntrega), puntosTotales,
                            claveComponente, porcentaje, archivos, enlaces, quitarAdjunto));
        } catch (IllegalArgumentException | IOException e) {
            return error(response, e instanceof IOException ? "No se pudo guardar el archivo adjunto." : e.getMessage());
        }
        notificar(response, publicacionId == null ? mensajeCreado(guardada.getTipo()) : "Cambios guardados");
        return panelDe(guardada.getTipo(), ctx, model);
    }

    @PostMapping("/clase/{claseId}/publicacion/{publicacionId}/eliminar")
    public String eliminarPublicacion(@PathVariable Long claseId, @PathVariable Long publicacionId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        TipoPublicacion tipo;
        try {
            tipo = classroomService.eliminarPublicacion(ctx, publicacionId);
        } catch (IllegalArgumentException e) {
            return error(response, e.getMessage());
        }
        notificar(response, "Publicación eliminada");
        return panelDe(tipo, ctx, model);
    }

    // ── Comentarios ───────────────────────────────────────────────────────────

    @PostMapping("/clase/{claseId}/publicacion/{publicacionId}/comentarios")
    public String comentar(@PathVariable Long claseId, @PathVariable Long publicacionId,
            @RequestParam(required = false) String contenido,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        try {
            model.addAttribute("pub", classroomService.comentar(ctx, publicacionId, contenido));
        } catch (IllegalArgumentException e) {
            return error(response, e.getMessage());
        }
        model.addAttribute("ctx", ctx);
        return FRAGMENTO_COMENTARIOS;
    }

    @PostMapping("/clase/{claseId}/comentario/{comentarioId}/eliminar")
    public String eliminarComentario(@PathVariable Long claseId, @PathVariable Long comentarioId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        try {
            model.addAttribute("pub", classroomService.eliminarComentario(ctx, comentarioId));
        } catch (IllegalArgumentException e) {
            return error(response, e.getMessage());
        }
        model.addAttribute("ctx", ctx);
        return FRAGMENTO_COMENTARIOS;
    }

    // ── Tareas: detalle, entrega y calificación ───────────────────────────────

    @GetMapping("/clase/{claseId}/tarea/{tareaId}")
    public String detalleTarea(@PathVariable Long claseId, @PathVariable Long tareaId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model) {
        return cargarDetalle(contexto(claseId, usuario, session), tareaId, model);
    }

    @PostMapping("/clase/{claseId}/tarea/{tareaId}/entregar")
    public String entregar(@PathVariable Long claseId, @PathVariable Long tareaId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) List<MultipartFile> archivos,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        try {
            classroomService.entregar(ctx, tareaId, texto, archivos);
        } catch (IllegalArgumentException | IOException e) {
            return error(response, e instanceof IOException ? "No se pudo guardar el archivo." : e.getMessage());
        }
        notificar(response, "Tarea entregada");
        return cargarDetalle(ctx, tareaId, model);
    }

    @PostMapping("/clase/{claseId}/tarea/{tareaId}/anular")
    public String anularEntrega(@PathVariable Long claseId, @PathVariable Long tareaId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        try {
            classroomService.anularEntrega(ctx, tareaId);
        } catch (IllegalArgumentException e) {
            return error(response, e.getMessage());
        }
        notificar(response, "Entrega anulada. Puedes modificarla y volver a entregar.");
        return cargarDetalle(ctx, tareaId, model);
    }

    @PostMapping("/clase/{claseId}/entrega/archivo/{adjuntoId}/quitar")
    public String quitarArchivoEntrega(@PathVariable Long claseId, @PathVariable Long adjuntoId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        Long tareaId;
        try {
            tareaId = classroomService.quitarArchivoEntrega(ctx, adjuntoId);
        } catch (IllegalArgumentException e) {
            return error(response, e.getMessage());
        }
        return cargarDetalle(ctx, tareaId, model);
    }

    @PostMapping("/clase/{claseId}/tarea/{tareaId}/calificar")
    public String calificar(@PathVariable Long claseId, @PathVariable Long tareaId,
            @RequestParam Long estudianteId,
            @RequestParam(required = false) String puntos,
            @RequestParam(required = false) String comentario,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session, Model model,
            HttpServletResponse response) {
        ContextoClase ctx = contexto(claseId, usuario, session);
        try {
            classroomService.calificar(ctx, tareaId, estudianteId, parseEntero(puntos), comentario);
        } catch (IllegalArgumentException e) {
            return error(response, e.getMessage());
        }
        // promedioDesactualizado: mismo evento que emite Gestión Académica al calificar
        response.setHeader("HX-Trigger", "{\"classroomGuardado\":{\"mensaje\":\"Trabajo devuelto al estudiante\"},"
                + "\"promedioDesactualizado\":\"\"}");
        return cargarDetalle(ctx, tareaId, model);
    }

    // ── Descarga de adjuntos ──────────────────────────────────────────────────

    @GetMapping("/adjunto/{adjuntoId}")
    public ResponseEntity<Resource> descargar(@PathVariable Long adjuntoId,
            @AuthenticationPrincipal CustomUserDetails usuario, HttpSession session) throws MalformedURLException {
        ClassroomAdjunto adjunto = classroomService.adjuntoParaDescarga(requerirDireccion(session), adjuntoId,
                usuario.getId(), usuario.getRoles());
        Path ruta = classroomService.rutaFisica(adjunto);
        if (!Files.isReadable(ruta)) {
            return ResponseEntity.notFound().build();
        }
        MediaType tipo = MediaTypeFactory.getMediaType(adjunto.getNombre()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        // PDF e imágenes se abren en el navegador; el resto se descarga
        boolean enLinea = MediaType.APPLICATION_PDF.includes(tipo) || "image".equals(tipo.getType());
        ContentDisposition disposicion = (enLinea ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(adjunto.getNombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new UrlResource(ruta.toUri()));
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private String panelDe(TipoPublicacion tipo, ContextoClase ctx, Model model) {
        return switch (tipo) {
            case ANUNCIO -> cargarTablon(ctx, model);
            case TAREA -> cargarTareas(ctx, model);
            case MATERIAL -> cargarMateriales(ctx, model);
        };
    }

    private String cargarTablon(ContextoClase ctx, Model model) {
        model.addAttribute("ctx", ctx);
        model.addAttribute("publicaciones", classroomService.tablon(ctx));
        model.addAttribute("proximas", classroomService.proximasEntregas(ctx));
        return FRAGMENTO_TABLON;
    }

    private String cargarTareas(ContextoClase ctx, Model model) {
        var grupos = classroomService.porTema(ctx, TipoPublicacion.TAREA);
        model.addAttribute("ctx", ctx);
        model.addAttribute("grupos", grupos);
        if (ctx.getRol().isEstudiante()) {
            model.addAttribute("misEntregas", classroomService.entregasPropias(ctx));
        } else {
            model.addAttribute("porRevisar", classroomService.entregasPorRevisar(ctx, grupos));
        }
        return FRAGMENTO_TAREAS;
    }

    private String cargarMateriales(ContextoClase ctx, Model model) {
        model.addAttribute("ctx", ctx);
        model.addAttribute("grupos", classroomService.porTema(ctx, TipoPublicacion.MATERIAL));
        return FRAGMENTO_MATERIALES;
    }

    private String cargarDetalle(ContextoClase ctx, Long tareaId, Model model) {
        ClassroomPublicacion tarea = classroomService.tarea(ctx, tareaId);
        model.addAttribute("ctx", ctx);
        model.addAttribute("tarea", tarea);
        if (ctx.getRol().isEstudiante()) {
            model.addAttribute("entrega", classroomService.entregaPropia(ctx, tarea));
            model.addAttribute("nota", classroomService.notaPropia(ctx, tarea));
        } else {
            model.addAttribute("filas", classroomService.filasEntrega(ctx, tarea));
        }
        return FRAGMENTO_DETALLE;
    }

    private ContextoClase contexto(Long claseId, CustomUserDetails usuario, HttpSession session) {
        return classroomService.contexto(requerirDireccion(session), claseId, usuario.getId(), usuario.getRoles());
    }

    private String mensajeCreado(TipoPublicacion tipo) {
        return switch (tipo) {
            case ANUNCIO -> "Anuncio publicado";
            case TAREA -> "Tarea asignada";
            case MATERIAL -> "Material publicado";
        };
    }

    // 422: htmx no reemplaza el contenido, así el formulario conserva lo escrito; el HX-Trigger sí se procesa.
    private String error(HttpServletResponse response, String mensaje) {
        response.setStatus(422);
        response.setHeader("HX-Trigger", "{\"classroomError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
        return "classroom/clase/comunes :: vacio";
    }

    private void notificar(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"classroomGuardado\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (id == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return id;
    }

    private LocalDateTime parseFechaHora(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha de entrega no válida");
        }
    }

    private Integer parseEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Los puntos deben ser un número entero");
        }
    }
}
