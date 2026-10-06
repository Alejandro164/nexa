package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/horario-periferica")
public class HorarioPerifericaController {

    @GetMapping
    public String index(HttpServletRequest request, HttpSession session) {
        requerirDireccion(session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "horario-periferica/index :: htmx-content";
        }
        return "horario-periferica/index";
    }

    private void requerirDireccion(HttpSession session) {
        if (session.getAttribute("SESSION_DIRECCION_ID") == null) {
            throw new DireccionNoSeleccionadaException();
        }
    }
}
