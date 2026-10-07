package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/grupos-por-area")
public class GruposPorAreaController {

    @GetMapping
    public String index(HttpServletRequest request) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "grupos-por-area/index :: htmx-content";
        }
        return "grupos-por-area/index";
    }
}
