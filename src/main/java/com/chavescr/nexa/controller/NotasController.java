package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.ObservacionGuiaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/notas")
public class NotasController {

    private final ObservacionGuiaService observacionGuiaService;

    public NotasController(ObservacionGuiaService observacionGuiaService) {
        this.observacionGuiaService = observacionGuiaService;
    }

    @PostMapping("/observacion")
    public String guardarObservacion(@RequestParam Long estudianteId, @RequestParam Long periodoId,
            @RequestParam(required = false) String texto, Model model, HttpSession session,
            HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        boolean supervision = request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
        Long autorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            observacionGuiaService.guardar(direccionId, autorId, supervision, estudianteId, periodoId, texto);
            String limpio = texto == null ? "" : texto.trim();
            model.addAttribute("ok", true);
            model.addAttribute("mensaje", limpio.isEmpty()
                    ? "Observación eliminada."
                    : "Observación guardada.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("ok", false);
            model.addAttribute("mensaje", e.getMessage());
        }
        return "notas/observacion-resultado :: resultado";
    }
}
