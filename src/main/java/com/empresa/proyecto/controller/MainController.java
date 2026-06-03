package com.empresa.proyecto.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.empresa.proyecto.dto.InstitucionDTO;
import com.empresa.proyecto.dto.UsuarioDTO;
import com.empresa.proyecto.service.InstitucionService;
import com.empresa.proyecto.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    private final UsuarioService usuarioService;
    private final InstitucionService institucionService;

    public MainController(UsuarioService usuarioService, InstitucionService institucionService) {
        this.usuarioService = usuarioService;
        this.institucionService = institucionService;
    }

    @GetMapping("/")
    public String index(Model model) {
        cargarDashboard(model);
        return "inicio/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(Model model, HttpServletRequest request) {
        cargarDashboard(model);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "inicio/inicio :: htmx-content";
        }
        return "inicio/inicio";
    }

    private void cargarDashboard(Model model) {
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
    }
}
