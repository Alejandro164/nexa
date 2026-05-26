package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    @GetMapping
    public String configuracion(Model model, HttpServletRequest request) {
        model.addAttribute("activeTab", "centro-educativo");
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion/index :: htmx-content";
        }
        return "configuracion/index";
    }
}
