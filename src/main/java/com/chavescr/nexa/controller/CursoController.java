package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CursoController {

    @GetMapping("/mis-cursos")
    public String misCursos(Model model, HttpServletRequest request) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "curso/mis-cursos :: htmx-content";
        }
        return "curso/mis-cursos";
    }
}
