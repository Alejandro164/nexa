package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @GetMapping("/calendario")
    public String calendario() {
        return "agenda/calendario/calendario :: content";
    }

    @GetMapping("/citas")
    public String citas() {
        return "agenda/citas/citas :: content";
    }

    @GetMapping("/salas")
    public String salas() {
        return "agenda/salas/salas :: content";
    }

    @GetMapping("/recordatorios")
    public String recordatorios() {
        return "agenda/recordatorio/recordatorios :: content";
    }
}
