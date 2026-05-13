package com.empresa.proyecto.controller;

import com.empresa.proyecto.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    private final UsuarioService usuarioService;

    public MainController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("usuarios", usuarioService.obtenerTodos());
        return "index";
    }

    @GetMapping("/usuarios/buscar")
    public String buscarUsuarios(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("usuarios", usuarioService.buscarPorNombre(q));
        return "fragments/tabla-usuarios :: tabla-contenido";
    }
}
