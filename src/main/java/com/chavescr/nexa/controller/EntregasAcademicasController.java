package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/entregas-academicas")
public class EntregasAcademicasController {

    @GetMapping
    public String index(HttpServletRequest request, HttpSession session) {
        requerirInstitucion(session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "entregas-academicas/index :: htmx-content";
        }
        return "entregas-academicas/index";
    }

    @GetMapping("/entregas")
    public String entregas(HttpSession session) {
        requerirInstitucion(session);
        return "entregas-academicas/entregas/entregas :: content";
    }

    @GetMapping("/pendientes")
    public String pendientes(HttpSession session) {
        requerirInstitucion(session);
        return "entregas-academicas/pendientes/pendientes :: content";
    }

    @GetMapping("/mantenimiento")
    public String mantenimiento(HttpSession session) {
        requerirInstitucion(session);
        return "entregas-academicas/mantenimiento/mantenimiento :: content";
    }

    private void requerirInstitucion(HttpSession session) {
        if (session.getAttribute("SESSION_INSTITUCION_ID") == null) {
            throw new InstitucionNoSeleccionadaException();
        }
    }
}
