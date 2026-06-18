package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/personal")
public class PersonalController {

    @GetMapping("/asistencia")
    public String asistencia() {
        return "personal/asistencia/asistencia :: content";
    }

    @GetMapping("/tareas")
    public String tareas() {
        return "personal/tareas/tareas :: content";
    }

    @GetMapping("/evaluacion")
    public String evaluacion() {
        return "personal/evaluacion/evaluacion :: content";
    }

    @GetMapping("/presencia")
    public String presencia() {
        return "personal/presencia/presencia :: content";
    }
}
