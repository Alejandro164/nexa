package com.empresa.proyecto.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.empresa.proyecto.dto.EmpresaDTO;
import com.empresa.proyecto.dto.UsuarioDTO;
import com.empresa.proyecto.service.EmpresaService;
import com.empresa.proyecto.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    private final UsuarioService usuarioService;
    private final EmpresaService empresaService;

    public MainController(UsuarioService usuarioService, EmpresaService empresaService) {
        this.usuarioService = usuarioService;
        this.empresaService = empresaService;
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
        List<EmpresaDTO> empresas = empresaService.obtenerTodasDTO();

        int totalUsuarios = usuarios.size();
        long usuariosActivos = usuarios.stream().filter(UsuarioDTO::isActivo).count();
        int totalEmpresas = empresas.size();
        long empresasActivas = empresas.stream().filter(EmpresaDTO::isActiva).count();
        int porcentajeActivos = totalUsuarios > 0
                ? (int) Math.round((double) usuariosActivos / totalUsuarios * 100)
                : 0;
        long totalRoles = usuarios.stream()
                .flatMap(u -> u.getRoles().stream())
                .distinct()
                .count();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("totalEmpresas", totalEmpresas);
        model.addAttribute("empresasActivas", empresasActivas);
        model.addAttribute("porcentajeActivos", porcentajeActivos);
        model.addAttribute("totalRoles", totalRoles);

        model.addAttribute("ultimosUsuarios",
                usuarios.stream()
                        .sorted((a, b) -> b.getId().compareTo(a.getId()))
                        .limit(5)
                        .toList());

        model.addAttribute("ultimasEmpresas",
                empresas.stream()
                        .sorted((a, b) -> b.getId().compareTo(a.getId()))
                        .limit(5)
                        .toList());
    }
}
