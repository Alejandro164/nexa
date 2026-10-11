package com.chavescr.nexa.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/impresiones")
public class ImpresionesController {

    @GetMapping
    public String index(HttpServletRequest request, HttpSession session, Model model) {
        requerirDireccion(session);
        model.addAttribute("tabInicial", puedeImprimir(request) ? "imprimir" : "solicitudes");
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "impresiones/index :: htmx-content";
        }
        return "impresiones/index";
    }

    @GetMapping("/imprimir")
    public String imprimir(HttpServletRequest request, HttpSession session) {
        requerirDireccion(session);
        if (!puedeImprimir(request)) {
            throw new AccessDeniedException("Solo el director o el administrador pueden ver la cola de impresión");
        }
        return "impresiones/imprimir/imprimir :: content";
    }

    @GetMapping("/solicitudes")
    public String solicitudes(HttpSession session) {
        requerirDireccion(session);
        return "impresiones/solicitudes/solicitudes :: content";
    }

    private boolean puedeImprimir(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
    }

    private void requerirDireccion(HttpSession session) {
        if (session.getAttribute("SESSION_DIRECCION_ID") == null) {
            throw new DireccionNoSeleccionadaException();
        }
    }
}
