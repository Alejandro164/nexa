package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ComponentesController {

    @GetMapping("/componentes")
    public String componentes() {
        return "componentes";
    }
}
