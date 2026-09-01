package com.chavescr.nexa.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;
import com.chavescr.nexa.service.OficioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/oficios")
public class OficioController {

    @Autowired
    private OficioService oficioService;

    @GetMapping
    public String oficios(Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = institucionId(session);
        if (institucionId != null) {
            cargarLista(model, institucionId, null);
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "oficios/index :: htmx-content";
        }
        return "oficios/index";
    }

    @GetMapping("/lista")
    public String lista(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarLista(model, institucionId(session), q);
        return "oficios/lista :: content";
    }

    @GetMapping("/form")
    public String formCrear(Model model, HttpSession session) {
        requerirInstitucion(session);
        model.addAttribute("oficio", new Oficio());
        return "oficios/formulario :: form-content";
    }

    @PostMapping
    public String crear(@RequestParam String asunto, @RequestParam String destinatario,
            @RequestParam(required = false) String numeroCircular,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            oficioService.crear(institucionId, usuarioId, asunto, destinatario, numeroCircular);
            cargarLista(model, institucionId, null);
            return "oficios/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#oficios-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            Oficio oficio = new Oficio();
            oficio.setAsunto(asunto);
            oficio.setDestinatario(destinatario);
            oficio.setNumeroCircular(numeroCircular);
            model.addAttribute("oficio", oficio);
            return "oficios/formulario :: form-content";
        }
    }

    @GetMapping("/{id}/subir-form")
    public String subirForm(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("oficio", oficioService.obtenerPorId(institucionId, id));
        return "oficios/subir-form :: form-content";
    }

    @PostMapping("/{id}/documento")
    public String subirDocumento(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            oficioService.subirDocumento(institucionId, id, archivo);
            cargarLista(model, institucionId, null);
            return "oficios/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#oficios-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("oficio", oficioService.obtenerPorId(institucionId, id));
            return "oficios/subir-form :: form-content";
        }
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<Resource> verDocumento(@PathVariable Long id, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Oficio oficio = oficioService.obtenerPorId(institucionId, id);
        if (oficio.getRutaArchivo() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(oficioService.getRutaRecursos()).resolve(oficio.getRutaArchivo());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + oficio.getNombreArchivoOriginal() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Error al abrir el documento: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, Model model, HttpSession session) throws IOException {
        Long institucionId = requerirInstitucion(session);
        oficioService.eliminar(institucionId, id);
        cargarLista(model, institucionId, null);
        return "oficios/lista :: content";
    }

    private void cargarLista(Model model, Long institucionId, String q) {
        List<Oficio> oficios = institucionId == null ? List.of() : oficioService.listar(institucionId, q);
        model.addAttribute("oficios", oficios);
        model.addAttribute("q", q);
    }

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = institucionId(session);
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }
}
