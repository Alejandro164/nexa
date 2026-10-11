package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/rendimiento-academico")
public class RendimientoAcademicoController {

    @GetMapping
    public String index(HttpServletRequest request, HttpSession session) {
        requerirDireccion(session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "rendimiento-academico/index :: htmx-content";
        }
        return "rendimiento-academico/index";
    }

    @GetMapping("/notas")
    public String notas(HttpSession session) {
        requerirDireccion(session);
        return "rendimiento-academico/notas/notas :: content";
    }

    @GetMapping("/asistencia")
    public String asistencia(HttpSession session) {
        requerirDireccion(session);
        return "rendimiento-academico/asistencia/asistencia :: content";
    }

    @GetMapping("/llamadas")
    public String llamadas(HttpSession session) {
        requerirDireccion(session);
        return "rendimiento-academico/llamadas/llamadas :: content";
    }

    @GetMapping("/boletas")
    public String boletas(HttpSession session) {
        requerirDireccion(session);
        return "rendimiento-academico/boletas/boletas :: content";
    }

    @GetMapping("/conducta")
    public String conducta(HttpSession session) {
        requerirDireccion(session);
        return "rendimiento-academico/conducta/conducta :: content";
    }

    @GetMapping("/apelaciones")
    public String apelaciones(HttpSession session) {
        requerirDireccion(session);
        return "rendimiento-academico/apelaciones/apelaciones :: content";
    }

    private void requerirDireccion(HttpSession session) {
        if (session.getAttribute("SESSION_DIRECCION_ID") == null) {
            throw new DireccionNoSeleccionadaException();
        }
    }
}
