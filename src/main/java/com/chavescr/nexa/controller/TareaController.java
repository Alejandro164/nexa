package com.chavescr.nexa.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.entity.AdjuntoTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo;
import com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo.RecordatorioTarea;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.TareaDetalleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Detalle de una tarea estilo Trello (modal): estado, etiquetas, fechas, checklist, miembros,
 * adjuntos y comentarios/actividad. Toda acción devuelve el cuerpo del modal ya actualizado;
 * los errores de validación se muestran dentro del mismo modal.
 */
@Controller
@RequestMapping("/agenda/tareas")
public class TareaController {

    @Autowired
    private TareaDetalleService tareaDetalleService;

    @FunctionalInterface
    private interface Accion {
        void ejecutar(Long direccionId, Long usuarioId) throws IOException;
    }

    @GetMapping("/{id}/detalle")
    public String detalle(@PathVariable Long id, Model model, HttpSession session, HttpServletRequest request) {
        cargar(model, id, session, request);
        return "agenda/tareas/detalle :: detalle-modal";
    }

    @PostMapping("/{id}/detalle/estado")
    public String estado(@PathVariable Long id, @RequestParam EstadoTarea estado,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.cambiarEstado(dir, usr, id, estado));
    }

    @PostMapping("/{id}/detalle/contenido")
    public String contenido(@PathVariable Long id, @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.actualizarContenido(dir, usr, id, titulo, descripcion));
    }

    @PostMapping("/{id}/detalle/fechas")
    public String fechas(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate limite,
            @RequestParam(required = false) RecordatorioTarea recordatorio,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.actualizarFechas(dir, usr, id, inicio, limite, recordatorio));
    }

    @PostMapping("/{id}/detalle/etiquetas/{etiquetaId}")
    public String alternarEtiqueta(@PathVariable Long id, @PathVariable Long etiquetaId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.alternarEtiqueta(dir, usr, id, etiquetaId));
    }

    @PostMapping("/{id}/detalle/etiquetas")
    public String crearEtiqueta(@PathVariable Long id, @RequestParam String nombre, @RequestParam String color,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.crearEtiqueta(dir, usr, id, nombre, color));
    }

    @DeleteMapping("/{id}/detalle/etiquetas/{etiquetaId}")
    public String eliminarEtiqueta(@PathVariable Long id, @PathVariable Long etiquetaId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.eliminarEtiqueta(dir, etiquetaId));
    }

    @PostMapping("/{id}/detalle/checklist")
    public String agregarItem(@PathVariable Long id, @RequestParam String texto,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.agregarItem(dir, usr, id, texto));
    }

    @PostMapping("/{id}/detalle/checklist/{itemId}")
    public String alternarItem(@PathVariable Long id, @PathVariable Long itemId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.alternarItem(dir, usr, id, itemId));
    }

    @DeleteMapping("/{id}/detalle/checklist/{itemId}")
    public String eliminarItem(@PathVariable Long id, @PathVariable Long itemId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.eliminarItem(dir, usr, id, itemId));
    }

    @PostMapping("/{id}/detalle/miembros/{usuarioId}")
    public String agregarColaborador(@PathVariable Long id, @PathVariable Long usuarioId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.agregarColaborador(dir, usr, id, usuarioId));
    }

    @DeleteMapping("/{id}/detalle/miembros/{usuarioId}")
    public String quitarColaborador(@PathVariable Long id, @PathVariable Long usuarioId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.quitarColaborador(dir, usr, id, usuarioId));
    }

    @PostMapping("/{id}/detalle/adjuntos")
    public String agregarAdjuntos(@PathVariable Long id,
            @RequestParam(required = false) List<MultipartFile> archivos,
            @RequestParam(required = false) String enlace,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.agregarAdjuntos(dir, usr, id, archivos, enlace));
    }

    @DeleteMapping("/{id}/detalle/adjuntos/{adjuntoId}")
    public String eliminarAdjunto(@PathVariable Long id, @PathVariable Long adjuntoId,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.eliminarAdjunto(dir, usr, id, adjuntoId));
    }

    @PostMapping("/{id}/detalle/comentarios")
    public String comentar(@PathVariable Long id, @RequestParam String texto,
            Model model, HttpSession session, HttpServletRequest request) {
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.comentar(dir, usr, id, texto));
    }

    @DeleteMapping("/{id}/detalle/comentarios/{entradaId}")
    public String eliminarComentario(@PathVariable Long id, @PathVariable Long entradaId,
            Model model, HttpSession session, HttpServletRequest request) {
        boolean supervisor = esSupervisor(request);
        return ejecutar(id, model, session, request,
                (dir, usr) -> tareaDetalleService.eliminarComentario(dir, usr, supervisor, id, entradaId));
    }

    // ─── Descarga de adjuntos ─────────────────────────────────────────────────

    @GetMapping("/adjuntos/{adjuntoId}")
    public ResponseEntity<Resource> descargar(@PathVariable Long adjuntoId, HttpSession session)
            throws MalformedURLException {
        AdjuntoTarea adjunto = tareaDetalleService.adjuntoParaDescarga(requerirDireccion(session), adjuntoId);
        Path ruta = tareaDetalleService.rutaFisica(adjunto);
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

    // ─── Internos ─────────────────────────────────────────────────────────────

    private String ejecutar(Long tareaId, Model model, HttpSession session, HttpServletRequest request, Accion accion) {
        try {
            accion.ejecutar(requerirDireccion(session), requerirUsuario(session));
        } catch (IllegalArgumentException | AccessDeniedException e) {
            model.addAttribute("error", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("error", "No se pudo guardar el archivo adjunto.");
        }
        cargar(model, tareaId, session, request);
        return "agenda/tareas/detalle :: detalle-cuerpo";
    }

    private void cargar(Model model, Long tareaId, HttpSession session, HttpServletRequest request) {
        Long direccionId = requerirDireccion(session);
        TareaPlanTrabajo tarea = tareaDetalleService.obtener(direccionId, tareaId);
        model.addAttribute("tarea", tarea);
        model.addAttribute("estados", EstadoTarea.values());
        model.addAttribute("recordatorios", RecordatorioTarea.values());
        model.addAttribute("etiquetasDisponibles", tareaDetalleService.listarEtiquetas(direccionId));
        model.addAttribute("candidatos", tareaDetalleService.listarCandidatos(direccionId, tarea));
        model.addAttribute("colores", TareaDetalleService.COLORES_ETIQUETA);
        model.addAttribute("usuarioActualId", requerirUsuario(session));
        model.addAttribute("puedeSupervisar", esSupervisor(request));
    }

    private boolean esSupervisor(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (id == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return id;
    }

    private Long requerirUsuario(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_USUARIO_ID");
        if (id == null) {
            throw new IllegalArgumentException("No hay usuario en sesión");
        }
        return id;
    }
}
