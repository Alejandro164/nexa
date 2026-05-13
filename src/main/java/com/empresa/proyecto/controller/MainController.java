package com.empresa.proyecto.controller;

import com.empresa.proyecto.service.EmpresaService;
import com.empresa.proyecto.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("usuarios", usuarioService.obtenerTodos());
        model.addAttribute("empresas", empresaService.obtenerTodas());
        return "index";
    }

    @GetMapping("/usuarios/buscar")
    public String buscarUsuarios(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("usuarios", usuarioService.buscarPorNombre(q));
        return "fragments/tabla-usuarios :: tabla-contenido";
    }

    @GetMapping("/centros/buscar")
    public String buscarCentros(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("empresas", empresaService.buscarPorNombre(q));
        return "fragments/tabla-centros :: tabla-contenido";
    }
}
