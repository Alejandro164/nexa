package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ComponentesController {

    @GetMapping("/componentes")
    public String componentes(HttpServletRequest request) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "componentes/componentes :: htmx-content";
        }
        return "componentes/componentes";
    }
}
