package com.chavescr.nexa.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.service.InstitucionService;
import com.chavescr.nexa.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    private final UsuarioService usuarioService;
    private final InstitucionService institucionService;

    public MainController(UsuarioService usuarioService, InstitucionService institucionService) {
        this.usuarioService = usuarioService;
        this.institucionService = institucionService;
    }

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request, HttpSession session) {

        if (session.getAttribute("institucionActivaId") != null) {
            cargarDashboard(model, session);
            return "inicio/inicio";
        }

        if (request.isUserInRole("ROLE_ADMIN")) {
            model.addAttribute("instituciones", institucionService.obtenerTodasDTO());
            return "inicio/lista-instituciones";
        }

        List<InstitucionDTO> instituciones = usuarioService.obtenerInstitucionesDelUsuarioActual();
        if (instituciones.isEmpty()) {
            model.addAttribute("instituciones", instituciones);
            model.addAttribute("errorNoInstituciones", "No tienes instituciones asociadas.");
            return "inicio/lista-instituciones";
        } else if (instituciones.size() == 1) {
            InstitucionDTO inst = instituciones.get(0);
            session.setAttribute("institucionActivaId", inst.getId());
            session.setAttribute("institucionActivaNombre", inst.getNombre());
            cargarDashboard(model, session);
            return "inicio/inicio";
        } else {
            model.addAttribute("instituciones", instituciones);
            return "inicio/lista-instituciones";
        }
    }

    @GetMapping("/inicio")
    public String inicio(Model model, HttpServletRequest request, HttpSession session) {
        cargarDashboard(model, session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "inicio/inicio :: htmx-content";
        }
        return "inicio/inicio";
    }

    @GetMapping("/inicio/instituciones-modal")
    public String institucionesModal(Model model, HttpServletRequest request) {

        System.out.println("Hola mundo");
        if (request.isUserInRole("ROLE_ADMIN")) {
            System.out.println("Admin");
            model.addAttribute("instituciones", institucionService.obtenerTodasDTO());
        } else {
            model.addAttribute("instituciones", usuarioService.obtenerInstitucionesDelUsuarioActual());
        }
        return "inicio/instituciones-modal :: modal-content";
    }

    @PostMapping("/inicio/cambiar-institucion")
    public String cambiarInstitucion(@RequestParam Long institucionId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        System.out.println("Institucion ID: " + institucionId);
        institucionService.findById(institucionId).ifPresent(inst -> {
            session.setAttribute("institucionActivaId", institucionId);
            session.setAttribute("institucionActivaNombre", inst.getNombre());
        });
        return "redirect:/inicio";
    }

    private void cargarDashboard(Model model, HttpSession session) {
        List<UsuarioDTO> usuarios = usuarioService.obtenerTodosDTO();
        List<InstitucionDTO> instituciones = institucionService.obtenerTodasDTO();

        int totalUsuarios = usuarios.size();
        long usuariosActivos = usuarios.stream().filter(UsuarioDTO::isActivo).count();
        int totalInstituciones = instituciones.size();
        long institucionesActivas = instituciones.stream().filter(InstitucionDTO::isActiva).count();
        int porcentajeActivos = totalUsuarios > 0
                ? (int) Math.round((double) usuariosActivos / totalUsuarios * 100)
                : 0;
        long totalRoles = usuarios.stream()
                .flatMap(u -> u.getRoles().stream())
                .distinct()
                .count();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("totalInstituciones", totalInstituciones);
        model.addAttribute("institucionesActivas", institucionesActivas);
        model.addAttribute("porcentajeActivos", porcentajeActivos);
        model.addAttribute("totalRoles", totalRoles);

        model.addAttribute("ultimosUsuarios",
                usuarios.stream()
                        .sorted((a, b) -> b.getId().compareTo(a.getId()))
                        .limit(5)
                        .toList());

        model.addAttribute("ultimasInstituciones",
                instituciones.stream()
                        .sorted((a, b) -> b.getId().compareTo(a.getId()))
                        .limit(5)
                        .toList());

        String institucionActivaNombre = (String) session.getAttribute("institucionActivaNombre");
        model.addAttribute("institucionActivaNombre", institucionActivaNombre);
    }
}
