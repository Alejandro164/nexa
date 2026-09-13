package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion-institucional")
public class ConfiguracionInstitucionalController {

    @GetMapping
    public String index(HttpServletRequest request, HttpSession session) {
        requerirInstitucion(session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion-institucional/index :: htmx-content";
        }
        return "configuracion-institucional/index";
    }

    @GetMapping("/jornada")
    public String jornada(HttpSession session) {
        requerirInstitucion(session);
        return "configuracion-institucional/jornada/jornada :: content";
    }

    @GetMapping("/componentes")
    public String componentes(HttpSession session) {
        requerirInstitucion(session);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @GetMapping("/bloqueo-leccion")
    public String bloqueoLeccion(HttpSession session) {
        requerirInstitucion(session);
        return "configuracion-institucional/bloqueo-leccion/bloqueo-leccion :: content";
    }

    private void requerirInstitucion(HttpSession session) {
        if (session.getAttribute("SESSION_INSTITUCION_ID") == null) {
            throw new InstitucionNoSeleccionadaException();
        }
    }
}
