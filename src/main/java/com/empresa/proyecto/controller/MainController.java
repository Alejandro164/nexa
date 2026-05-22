package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.empresa.proyecto.service.EmpresaService;
import com.empresa.proyecto.service.UsuarioService;

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
        return "index";
    }


}
