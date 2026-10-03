package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.EscalaNotasService;
import com.chavescr.nexa.service.NotasConsultaService;
import com.chavescr.nexa.service.ObservacionGuiaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/notas")
public class NotasController {

    private final ObservacionGuiaService observacionGuiaService;
    private final NotasConsultaService notasConsultaService;
    private final EscalaNotasService escalaNotasService;

    public NotasController(ObservacionGuiaService observacionGuiaService,
            NotasConsultaService notasConsultaService, EscalaNotasService escalaNotasService) {
        this.observacionGuiaService = observacionGuiaService;
        this.notasConsultaService = notasConsultaService;
        this.escalaNotasService = escalaNotasService;
    }

    @GetMapping("/desglose")
    public String desglose(@RequestParam Long estudianteId, @RequestParam Long periodoId, Model model,
            HttpSession session, HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        boolean supervision = request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        model.addAttribute("escala", escalaNotasService.vista(direccionId));
        try {
            model.addAttribute("filas", notasConsultaService.desglose(
                    direccionId, usuarioId, supervision, estudianteId, periodoId));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "notas/detalle-por-componente :: filas";
    }

    @GetMapping("/ausencias")
    public String ausencias(@RequestParam Long estudianteId, @RequestParam Long periodoId, Model model,
            HttpSession session, HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        boolean supervision = request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            model.addAttribute("ausencias", notasConsultaService.ausencias(
                    direccionId, usuarioId, supervision, estudianteId, periodoId));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "notas/detalle-ausencias :: registros";
    }

    @GetMapping("/llamadas")
    public String llamadas(@RequestParam Long estudianteId, @RequestParam Long periodoId, Model model,
            HttpSession session, HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        boolean supervision = request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            model.addAttribute("llamadas", notasConsultaService.llamadas(
                    direccionId, usuarioId, supervision, estudianteId, periodoId));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "notas/detalle-llamadas :: registros";
    }

    @GetMapping("/boletas")
    public String boletas(@RequestParam Long estudianteId, @RequestParam Long periodoId, Model model,
            HttpSession session, HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        boolean supervision = request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            model.addAttribute("boletas", notasConsultaService.boletas(
                    direccionId, usuarioId, supervision, estudianteId, periodoId));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "notas/detalle-boletas :: registros";
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
