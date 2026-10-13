package com.chavescr.nexa.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.entity.TipoDestinatario;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.ComunicadoService;
import com.chavescr.nexa.service.OficioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/oficios")
public class OficioController {

    @Autowired
    private OficioService oficioService;

    @Autowired
    private ComunicadoService comunicadoService;

    @GetMapping
    public String oficios(Model model, HttpSession session, HttpServletRequest request) {
        Long direccionId = direccionId(session);
        if (direccionId != null) {
            cargarLista(model, direccionId, null);
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "oficios/index :: htmx-content";
        }
        return "oficios/index";
    }

    @GetMapping("/lista")
    public String lista(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarLista(model, direccionId(session), q);
        return "oficios/lista :: content";
    }

    @GetMapping("/form")
    public String formCrear(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("oficio", new Oficio());
        model.addAttribute("destinatarioIdSeleccionado", null);
        cargarOpcionesDestinatario(model);
        cargarDestinatariosInternos(model, direccionId, Set.of(), false);
        return "oficios/formulario :: form-content";
    }

    @GetMapping("/form/{id}")
    public String formEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Oficio oficio = oficioService.obtenerPorId(direccionId, id);
        model.addAttribute("oficio", oficio);
        model.addAttribute("destinatarioIdSeleccionado",
                oficio.getDestinatarioDireccion() != null ? oficio.getDestinatarioDireccion().getId() : null);
        cargarOpcionesDestinatario(model);
        cargarDestinatariosInternos(model, direccionId, comunicadoService.clavesDe(oficio),
                "EMITIDO".equals(oficio.getEstado()));
        return "oficios/formulario :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam(required = false) Long id,
            @RequestParam String asunto,
            @RequestParam(required = false) Long destinatarioDireccionId,
            @RequestParam(required = false) String numeroCircular,
            @RequestParam(name = "destinatarios", required = false) List<String> destinatarios,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            if (id == null) {
                Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
                oficioService.crear(direccionId, usuarioId, asunto, destinatarioDireccionId, numeroCircular, destinatarios);
            } else {
                oficioService.actualizar(direccionId, id, asunto, destinatarioDireccionId, numeroCircular, destinatarios);
            }
            cargarLista(model, direccionId, null);
            return "oficios/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#oficios-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            Oficio oficio = new Oficio();
            oficio.setId(id);
            oficio.setAsunto(asunto);
            oficio.setNumeroCircular(numeroCircular);
            model.addAttribute("oficio", oficio);
            model.addAttribute("destinatarioIdSeleccionado", destinatarioDireccionId);
            cargarOpcionesDestinatario(model);
            cargarDestinatariosInternos(model, direccionId,
                    destinatarios == null ? Set.of() : new LinkedHashSet<>(destinatarios), esEmitido(direccionId, id));
            return "oficios/formulario :: form-content";
        }
    }

    /** Búsqueda de personas para elegirlas como destinatarias (la usa el formulario mientras se escribe). */
    @GetMapping("/personas")
    public String personas(@RequestParam(name = "q", required = false) String busqueda, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("personasEncontradas", comunicadoService.buscarPersonas(direccionId, busqueda));
        return "oficios/formulario :: personas";
    }

    @PostMapping("/direcciones")
    public String registrarDireccion(@RequestParam String nuevaDireccionNombre,
            @RequestParam(required = false) String nuevaDireccionCedula,
            @RequestParam(required = false) String nuevaDireccionEmail,
            Model model, HttpServletResponse response) {
        try {
            Direccion nueva = oficioService.registrarDireccionDestinataria(nuevaDireccionNombre,
                    nuevaDireccionCedula, nuevaDireccionEmail);
            model.addAttribute("direccionesDestinatario", oficioService.listarDireccionesActivas());
            model.addAttribute("destinatarioIdSeleccionado", nueva.getId());
            return "oficios/formulario :: wrap-destinatario-direccion";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#error-nueva-direccion");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorDireccion", e.getMessage());
            return "oficios/formulario :: error-nueva-direccion";
        }
    }

    @GetMapping("/{id}/subir-form")
    public String subirForm(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("oficio", oficioService.obtenerPorId(direccionId, id));
        return "oficios/subir-form :: form-content";
    }

    @PostMapping("/{id}/documento")
    public String subirDocumento(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            oficioService.subirDocumento(direccionId, id, usuarioId, archivo);
            cargarLista(model, direccionId, null);
            return "oficios/lista :: content";
        } catch (Exception e) {
            notificarError(response, "No se pudo subir el documento: " + e.getMessage());

            Oficio oficio = null;
            try {
                oficio = oficioService.obtenerPorId(direccionId, id);
            } catch (Exception noEncontrado) {
                // El oficio ya no existe (p. ej. lo eliminaron mientras se subía el documento):
                // no hay formulario sensato que re-mostrar, se refresca la lista en su lugar.
            }
            if (oficio == null) {
                cargarLista(model, direccionId, null);
                return "oficios/lista :: content";
            }

            response.setHeader("HX-Retarget", "#oficios-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("oficio", oficio);
            return "oficios/subir-form :: form-content";
        }
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        // Los encabezados HTTP no admiten saltos de línea (Tomcat los rechaza como posible header
        // injection) — algunos mensajes de excepción (ej. fallos de conexión SMTP) vienen multilínea.
        String mensajeSaneado = mensaje.replace("\"", "'").replaceAll("[\\r\\n]+", " ").trim();
        response.setHeader("HX-Trigger", "{\"oficioError\":{\"mensaje\":\"" + mensajeSaneado + "\"}}");
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<Resource> verDocumento(@PathVariable Long id, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Oficio oficio = oficioService.obtenerPorId(direccionId, id);
        if (oficio.getNubeNodo() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(oficioService.getRutaRecursos()).resolve(oficio.getNubeNodo().getUrlArchivo());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + oficio.getNubeNodo().getNombre() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Error al abrir el documento: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            oficioService.eliminar(direccionId, id);
        } catch (Exception e) {
            notificarError(response, e.getMessage());
        }
        cargarLista(model, direccionId, null);
        return "oficios/lista :: content";
    }

    @PostMapping("/{id}/emitir")
    public String emitir(@PathVariable Long id, Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            oficioService.emitir(direccionId, id);
        } catch (Exception e) {
            notificarError(response, "No se pudo emitir el oficio: " + e.getMessage());
        }
        cargarLista(model, direccionId, null);
        return "oficios/lista :: content";
    }

    private void cargarLista(Model model, Long direccionId, String q) {
        List<Oficio> oficios = direccionId == null ? List.of() : oficioService.listar(direccionId, q);
        model.addAttribute("oficios", oficios);
        model.addAttribute("q", q);
    }

    private void cargarOpcionesDestinatario(Model model) {
        model.addAttribute("direccionesDestinatario", oficioService.listarDireccionesActivas());
    }

    /** Lo que el formulario necesita para elegir destinatarios internos (grupos, secciones y personas ya elegidas). */
    private void cargarDestinatariosInternos(Model model, Long direccionId, Set<String> seleccion, boolean bloqueado) {
        model.addAttribute("gruposSimples", Arrays.stream(TipoDestinatario.values())
                .filter(TipoDestinatario::esGrupoSimple).toList());
        model.addAttribute("secciones", comunicadoService.secciones(direccionId));
        model.addAttribute("seleccion", seleccion);
        model.addAttribute("personasElegidas", comunicadoService.personasElegidas(direccionId, seleccion));
        model.addAttribute("destinatariosBloqueados", bloqueado);
    }

    private boolean esEmitido(Long direccionId, Long id) {
        if (id == null) {
            return false;
        }
        try {
            return "EMITIDO".equals(oficioService.obtenerPorId(direccionId, id).getEstado());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Long direccionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_DIRECCION_ID");
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = direccionId(session);
        if (id == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return id;
    }
}
