package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.DistribucionPorcentualService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/distribucion")
public class DistribucionPorcentualController {

    private final DistribucionPorcentualService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public DistribucionPorcentualController(DistribucionPorcentualService service,
            AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String distribucion(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long direccionId = requerirDireccion(session);
        cargarPanel(model, direccionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/distribucion/distribucion :: content";
    }

    @PostMapping
    public String guardar(@RequestParam Long periodoId,
            @RequestParam Long materiaId,
            @RequestParam(required = false) Integer cotidiano,
            @RequestParam(required = false) Integer tareas,
            @RequestParam(required = false) Integer proyectos,
            @RequestParam(required = false) Integer examenes,
            @RequestParam(required = false) Integer asistencia,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            service.guardarDistribucion(direccionId, periodoId, materiaId, cotidiano, tareas, proyectos,
                    examenes, asistencia);
            model.addAttribute("guardadoOk", true);
            response.setHeader("HX-Trigger", "promedioDesactualizado");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, direccionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/distribucion/distribucion :: content";
    }

    private void cargarPanel(Model model, Long direccionId, Long periodoId, Long materiaId, Long docenteId) {
        var periodos = service.listarPeriodosActivos(direccionId);
        var materias = alcanceDocenteService.materiasVisibles(direccionId, docenteId);
        if (periodoId == null && !periodos.isEmpty()) {
            periodoId = periodos.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }
        model.addAttribute("periodos", periodos);
        model.addAttribute("materias", materias);
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        if (periodoId != null && materiaId != null) {
            model.addAttribute("distribucion", service.obtenerDistribucion(direccionId, periodoId, materiaId));
        }
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (id == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return id;
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
