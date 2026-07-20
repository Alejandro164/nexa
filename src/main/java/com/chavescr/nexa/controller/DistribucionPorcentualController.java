package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.service.DistribucionPorcentualService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/distribucion")
public class DistribucionPorcentualController {

    private final DistribucionPorcentualService service;

    public DistribucionPorcentualController(DistribucionPorcentualService service) {
        this.service = service;
    }

    @GetMapping
    public String distribucion(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, periodoId, materiaId);
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
            @RequestParam(required = false) Integer trabajosExtraclase,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.guardarDistribucion(institucionId, periodoId, materiaId, cotidiano, tareas, proyectos,
                    examenes, asistencia, trabajosExtraclase);
            model.addAttribute("guardadoOk", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, institucionId, periodoId, materiaId);
        return "gestion-academica/distribucion/distribucion :: content";
    }

    private void cargarPanel(Model model, Long institucionId, Long periodoId, Long materiaId) {
        var periodos = service.listarPeriodosActivos(institucionId);
        var materias = service.listarMateriasActivas(institucionId);
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
            model.addAttribute("distribucion", service.obtenerDistribucion(institucionId, periodoId, materiaId));
        }
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }
}
