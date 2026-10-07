package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/rendimiento-academico")
public class RendimientoAcademicoController {

    @GetMapping
    public String index(HttpServletRequest request) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "rendimiento-academico/index :: htmx-content";
        }
        return "rendimiento-academico/index";
    }
}
