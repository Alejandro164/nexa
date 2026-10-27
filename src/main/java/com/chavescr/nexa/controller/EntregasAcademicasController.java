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

import com.chavescr.nexa.dto.FilaEntregaAcademica;
import com.chavescr.nexa.entity.AdjuntoEntregaAcademica;
import com.chavescr.nexa.entity.EntregaAcademica;
import com.chavescr.nexa.entity.EntregaAcademica.TipoEntrega;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.EntregaAcademicaService;
import com.chavescr.nexa.service.EntregaAcademicaService.FiltroEntregas;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Entregas académicas. Los docentes ven y presentan sus entregas (Pendientes / Realizadas); la dirección
 * (administrador y director) define las entregas (Mantenimiento), ve el seguimiento de todos los docentes y
 * revisa lo presentado. Las acciones del detalle devuelven el cuerpo del modal ya actualizado y los errores de
 * validación se muestran dentro del mismo modal.
 */
@Controller
@RequestMapping("/entregas-academicas")
public class EntregasAcademicasController {

    private static final String TABLA = "entregas-tabla";

    @Autowired
    private EntregaAcademicaService entregaService;

    @FunctionalInterface
    private interface Accion {
        void ejecutar(Long direccionId, Long usuarioId) throws IOException;
    }

    // ─── Shell y pestañas ─────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model, HttpServletRequest request, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("supervisor", esSupervisor(request));
        model.addAttribute("pendientesCount",
                entregaService.contarPendientes(direccionId, requerirUsuario(session), esSupervisor(request)));
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "entregas-academicas/index :: htmx-content";
        }
        return "entregas-academicas/index";
    }

    @GetMapping("/pendientes")
    public String pendientes(@RequestParam(required = false) Long entregaId,
            @RequestParam(required = false) String tipo, @RequestParam(required = false) Long periodoId,
            @RequestParam(defaultValue = "1") int pagina, Model model, HttpSession session,
            HttpServletRequest request) {
        cargarLista(model, true, entregaId, tipo, periodoId, pagina, session, request);
        return TABLA.equals(request.getHeader("HX-Target"))
                ? "entregas-academicas/comun :: tabla-con-contador"
                : "entregas-academicas/pendientes/pendientes :: content";
    }

    @GetMapping("/realizadas")
    public String realizadas(@RequestParam(required = false) Long entregaId,
            @RequestParam(required = false) String tipo, @RequestParam(required = false) Long periodoId,
            @RequestParam(defaultValue = "1") int pagina, Model model, HttpSession session,
            HttpServletRequest request) {
        cargarLista(model, false, entregaId, tipo, periodoId, pagina, session, request);
        return TABLA.equals(request.getHeader("HX-Target"))
                ? "entregas-academicas/comun :: tabla-con-contador"
                : "entregas-academicas/realizadas/realizadas :: content";
    }

    // ─── Detalle de una entrega (modal) ───────────────────────────────────────

    @GetMapping("/detalle/{entregaId}")
    public String detalle(@PathVariable Long entregaId, @RequestParam(required = false) Long docenteId,
            Model model, HttpSession session, HttpServletRequest request) {
        cargarDetalle(model, entregaId, docenteId, session, request);
        return "entregas-academicas/detalle :: detalle-modal";
    }

    @PostMapping("/detalle/{entregaId}/borrador")
    public String guardarBorrador(@PathVariable Long entregaId, @RequestParam(required = false) String observacion,
            Model model, HttpSession session, HttpServletRequest request) {
        return docente(entregaId, model, session, request,
                (dir, usr) -> entregaService.guardarBorrador(dir, usr, entregaId, observacion));
    }

    @PostMapping("/detalle/{entregaId}/adjuntos")
    public String agregarAdjuntos(@PathVariable Long entregaId,
            @RequestParam(required = false) List<MultipartFile> archivos,
            @RequestParam(required = false) String enlace,
            @RequestParam(required = false) String observacion,
            Model model, HttpSession session, HttpServletRequest request) {
        return docente(entregaId, model, session, request, (dir, usr) -> {
            // El texto escrito en la observación se conserva aunque todavía no se haya guardado el borrador
            if (observacion != null) {
                entregaService.guardarBorrador(dir, usr, entregaId, observacion);
            }
            entregaService.agregarAdjuntos(dir, usr, entregaId, archivos, enlace);
        });
    }

    @PostMapping("/detalle/{entregaId}/adjuntos/{adjuntoId}/eliminar")
    public String eliminarAdjunto(@PathVariable Long entregaId, @PathVariable Long adjuntoId,
            @RequestParam(required = false) String observacion,
            Model model, HttpSession session, HttpServletRequest request) {
        return docente(entregaId, model, session, request, (dir, usr) -> {
            if (observacion != null) {
                entregaService.guardarBorrador(dir, usr, entregaId, observacion);
            }
            entregaService.eliminarAdjunto(dir, usr, entregaId, adjuntoId);
        });
    }

    @PostMapping("/detalle/{entregaId}/presentar")
    public String presentar(@PathVariable Long entregaId, @RequestParam(required = false) String observacion,
            Model model, HttpSession session, HttpServletRequest request) {
        return docente(entregaId, model, session, request,
                (dir, usr) -> entregaService.presentar(dir, usr, entregaId, observacion));
    }

    /** La dirección aprueba la entrega presentada o la devuelve con observaciones. */
    @PostMapping("/detalle/{entregaId}/revision")
    public String revisar(@PathVariable Long entregaId, @RequestParam Long presentacionId,
            @RequestParam boolean aprobar, @RequestParam(required = false) String observacion,
            @RequestParam(required = false) Long docenteId,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        try {
            entregaService.revisar(requerirDireccion(session), requerirUsuario(session), presentacionId, aprobar,
                    observacion);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarDetalle(model, entregaId, docenteId, session, request);
        return "entregas-academicas/detalle :: detalle-cuerpo";
    }

    @GetMapping("/adjuntos/{adjuntoId}")
    public ResponseEntity<Resource> descargar(@PathVariable Long adjuntoId, HttpSession session,
            HttpServletRequest request) throws MalformedURLException {
        AdjuntoEntregaAcademica adjunto = entregaService.adjuntoParaDescarga(requerirDireccion(session),
                requerirUsuario(session), esSupervisor(request), adjuntoId);
        Path ruta = entregaService.rutaFisica(adjunto);
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

    // ─── Mantenimiento (solo dirección) ───────────────────────────────────────

    @GetMapping("/mantenimiento")
    public String mantenimiento(Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        cargarMantenimiento(model, session);
        return "entregas-academicas/mantenimiento/mantenimiento :: content";
    }

    @GetMapping("/mantenimiento/form")
    public String formCrear(Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        EntregaAcademica entrega = new EntregaAcademica();
        entrega.setFechaInicio(LocalDate.now());
        entrega.setFechaFin(LocalDate.now().plusDays(14));
        return formulario(model, session, entrega);
    }

    @GetMapping("/mantenimiento/form/{id}")
    public String formEditar(@PathVariable Long id, Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        return formulario(model, session, entregaService.obtenerEntrega(requerirDireccion(session), id));
    }

    @PostMapping("/mantenimiento")
    public String guardar(@RequestParam(required = false) Long id, @RequestParam String nombre,
            @RequestParam(required = false) String descripcion, @RequestParam(required = false) TipoEntrega tipo,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String activa,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirSupervisor(request);
        Long direccionId = requerirDireccion(session);
        try {
            entregaService.guardarEntrega(direccionId, requerirUsuario(session), id, nombre, descripcion, tipo,
                    periodoId, fechaInicio, fechaFin, activa != null);
            cargarMantenimiento(model, session);
            return "entregas-academicas/mantenimiento/mantenimiento :: content";
        } catch (IllegalArgumentException e) {
            response.setHeader("HX-Retarget", "#entregas-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            EntregaAcademica entrega = new EntregaAcademica();
            entrega.setId(id);
            entrega.setNombre(nombre);
            entrega.setDescripcion(descripcion);
            entrega.setTipo(tipo != null ? tipo : TipoEntrega.OTRA);
            entrega.setFechaInicio(fechaInicio);
            entrega.setFechaFin(fechaFin);
            entrega.setActiva(activa != null);
            return formulario(model, session, entrega);
        }
    }

    @PostMapping("/mantenimiento/{id}/activa")
    public String alternarActiva(@PathVariable Long id, Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        entregaService.alternarActiva(requerirDireccion(session), id);
        cargarMantenimiento(model, session);
        return "entregas-academicas/mantenimiento/mantenimiento :: content";
    }

    @DeleteMapping("/mantenimiento/{id}")
    public String eliminar(@PathVariable Long id, Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        try {
            entregaService.eliminarEntrega(requerirDireccion(session), id);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorLista", e.getMessage());
        }
        cargarMantenimiento(model, session);
        return "entregas-academicas/mantenimiento/mantenimiento :: content";
    }

    @GetMapping("/mantenimiento/{id}/seguimiento")
    public String seguimiento(@PathVariable Long id, Model model, HttpSession session, HttpServletRequest request) {
        exigirSupervisor(request);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("entrega", entregaService.obtenerEntrega(direccionId, id));
        model.addAttribute("filas", entregaService.seguimiento(direccionId, id));
        return "entregas-academicas/mantenimiento/seguimiento :: seguimiento-modal";
    }

    // ─── Internos ─────────────────────────────────────────────────────────────

    private void cargarLista(Model model, boolean pendientes, Long entregaId, String tipo, Long periodoId, int pagina,
            HttpSession session, HttpServletRequest request) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        boolean supervisor = esSupervisor(request);
        TipoEntrega tipoFiltro = null;
        if (tipo != null && !tipo.isBlank()) {
            try {
                tipoFiltro = TipoEntrega.valueOf(tipo);
            } catch (IllegalArgumentException e) {
                tipoFiltro = null;
            }
        }
        model.addAttribute("pagina", entregaService.listar(direccionId, usuarioId, supervisor, pendientes,
                new FiltroEntregas(entregaId, tipoFiltro, periodoId), pagina));
        model.addAttribute("modo", pendientes ? "pendientes" : "realizadas");
        model.addAttribute("supervisor", supervisor);
        model.addAttribute("entregasFiltro", entregaService.listarEntregas(direccionId));
        model.addAttribute("periodos", entregaService.listarPeriodos(direccionId));
        model.addAttribute("tipos", TipoEntrega.values());
        model.addAttribute("filtroEntregaId", entregaId);
        model.addAttribute("filtroTipo", tipoFiltro != null ? tipoFiltro.name() : "");
        model.addAttribute("filtroPeriodoId", periodoId);
        model.addAttribute("pendientesCount", entregaService.contarPendientes(direccionId, usuarioId, supervisor));
    }

    private void cargarMantenimiento(Model model, HttpSession session) {
        model.addAttribute("entregas", entregaService.listarEntregas(requerirDireccion(session)));
    }

    private String formulario(Model model, HttpSession session, EntregaAcademica entrega) {
        model.addAttribute("entrega", entrega);
        model.addAttribute("tipos", TipoEntrega.values());
        model.addAttribute("periodos", entregaService.listarPeriodos(requerirDireccion(session)));
        return "entregas-academicas/mantenimiento/formulario :: form-content";
    }

    /** Acción propia del docente sobre su respuesta; no aplica a quien solo supervisa. */
    private String docente(Long entregaId, Model model, HttpSession session, HttpServletRequest request,
            Accion accion) {
        if (esSupervisor(request)) {
            throw new AccessDeniedException("La dirección revisa las entregas, no las presenta");
        }
        try {
            accion.ejecutar(requerirDireccion(session), requerirUsuario(session));
        } catch (IllegalArgumentException | AccessDeniedException e) {
            model.addAttribute("error", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("error", "No se pudo guardar el archivo adjunto.");
        }
        cargarDetalle(model, entregaId, null, session, request);
        return "entregas-academicas/detalle :: detalle-cuerpo";
    }

    private void cargarDetalle(Model model, Long entregaId, Long docenteId, HttpSession session,
            HttpServletRequest request) {
        boolean supervisor = esSupervisor(request);
        FilaEntregaAcademica fila = entregaService.detalle(requerirDireccion(session), requerirUsuario(session),
                supervisor, entregaId, docenteId);
        boolean editable = !supervisor && fila.getEntrega().isActiva()
                && (fila.getPresentacion() == null || fila.getPresentacion().isEditable());
        model.addAttribute("fila", fila);
        model.addAttribute("supervisor", supervisor);
        model.addAttribute("editable", editable);
        model.addAttribute("puedeRevisar", supervisor && fila.getPresentacion() != null
                && fila.getPresentacion().isPorRevisar());
    }

    private boolean esSupervisor(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
    }

    private void exigirSupervisor(HttpServletRequest request) {
        if (!esSupervisor(request)) {
            throw new AccessDeniedException("Solo el administrador o el director gestionan las entregas académicas");
        }
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
