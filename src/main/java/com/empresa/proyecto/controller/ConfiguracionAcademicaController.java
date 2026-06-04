package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/configuracion-academica")
public class ConfiguracionAcademicaController {

    @GetMapping
    public String configuracionAcademica(Model model, HttpServletRequest request) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion-academica/index :: htmx-content";
        }
        return "configuracion-academica/index";
    }
}
