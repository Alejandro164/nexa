package com.chavescr.nexa.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.InstitucionService;
import com.chavescr.nexa.service.SesionInstitucionService;
import com.chavescr.nexa.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private InstitucionService institucionService;

    @Autowired
    private SesionInstitucionService sesionInstitucionService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails usuario, Model model,
            HttpServletRequest request, HttpSession session) {

        session.setAttribute("SESSION_USUARIO_ID", usuario.getId());

        var resultado = sesionInstitucionService.resolver(session, request.isUserInRole("ROLE_ADMIN"));
        if (resultado.estado() != SesionInstitucionService.Estado.RESUELTA) {
            // La selección de institución ahora se resuelve en el login (modal por AJAX); si se
            // llega aquí sin institución resuelta (JS deshabilitado, navegación directa a /, etc.)
            // se cierra la sesión y se manda de vuelta al login para que pase por ese flujo.
            session.invalidate();
            return "redirect:/login";
        }

        cargarDashboard(model, session);
        return "inicio/inicio";
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
    public String institucionesModal(@RequestParam(required = false) String origen, Model model,
            HttpServletRequest request, HttpSession session) {
        if (request.isUserInRole("ROLE_ADMIN")) {
            model.addAttribute("instituciones", institucionService.obtenerTodasDTO());
        } else {
            model.addAttribute("instituciones", usuarioService.obtenerInstitucionesDelUsuarioActual());
        }
        model.addAttribute("institucionActualId", session.getAttribute("SESSION_INSTITUCION_ID"));
        if ("login".equals(origen)) {
            return "auth/seleccionar-institucion-modal :: modal-content";
        }
        return "inicio/instituciones-modal :: modal-content";
    }

    @PostMapping("/inicio/cambiar-institucion")
    public String cambiarInstitucion(@RequestParam Long institucionId,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");

        if (request.isUserInRole("ROLE_ADMIN")) {
            institucionService.findById(institucionId).ifPresent(inst -> {
                session.setAttribute("SESSION_INSTITUCION_ID", institucionId);
                session.setAttribute("SESSION_INSTITUCION_NOMBRE", inst.getNombre());
                usuarioService.actualizarUltimaInstitucion(usuarioId, inst);
            });
            return "redirect:/inicio";
        }

        usuarioService.obtenerInstitucionesDelUsuarioActual().stream()
                .filter(inst -> inst.getId().equals(institucionId))
                .findFirst()
                .ifPresent(inst -> {
                    session.setAttribute("SESSION_INSTITUCION_ID", institucionId);
                    session.setAttribute("SESSION_INSTITUCION_NOMBRE", inst.getNombre());
                    institucionService.findById(institucionId)
                            .ifPresent(entidad -> usuarioService.actualizarUltimaInstitucion(usuarioId, entidad));
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

        String institucionActivaNombre = (String) session.getAttribute("SESSION_INSTITUCION_NOMBRE");
        model.addAttribute("institucionActivaNombre", institucionActivaNombre);
    }
}
