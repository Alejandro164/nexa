package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/docentes")
public class DocentesController {

    @GetMapping
    public String docentes(Model model, HttpServletRequest request) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "docentes/index :: htmx-content";
        }
        return "docentes/index";
    }
}
