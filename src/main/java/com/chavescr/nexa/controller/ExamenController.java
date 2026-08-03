package com.chavescr.nexa.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.Examen;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.ExamenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/examenes")
public class ExamenController {

    private final ExamenService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public ExamenController(ExamenService service, AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String examenes(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/examenes/examenes :: content";
    }

    @GetMapping("/form")
    public String nuevoExamen(@RequestParam Long periodoId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("examen", new Examen());
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/examenes/form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarExamen(@PathVariable Long id, @RequestParam Long periodoId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("examen", service.obtenerExamen(institucionId, id));
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/examenes/form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long periodoId, @RequestParam Long materiaId,
            @ModelAttribute Examen examen, Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        service.guardarExamen(institucionId, periodoId, materiaId, examen);
        cargarPanel(model, institucionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/examenes/examenes :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long periodoId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarExamen(institucionId, id);
        cargarPanel(model, institucionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/examenes/examenes :: content";
    }

    @GetMapping("/{id}/notas")
    public String notas(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarNotas(model, institucionId, id);
        return "gestion-academica/examenes/notas :: form-content";
    }

    @PostMapping("/{id}/notas")
    public String guardarNota(@PathVariable Long id, @RequestParam Long estudianteId,
            @RequestParam Integer calificacion, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.guardarNota(institucionId, id, estudianteId, calificacion);
        cargarNotas(model, institucionId, id);
        return "gestion-academica/examenes/notas :: form-content";
    }

    private void cargarNotas(Model model, Long institucionId, Long examenId) {
        model.addAttribute("examen", service.obtenerExamen(institucionId, examenId));
        model.addAttribute("filas", service.listarNotas(institucionId, examenId));
    }

    private void cargarPanel(Model model, Long institucionId, Long periodoId, Long materiaId, Long docenteId) {
        var periodos = service.listarPeriodosActivos(institucionId);
        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
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
        model.addAttribute("examenes", periodoId != null && materiaId != null
                ? service.listarExamenes(institucionId, periodoId, materiaId)
                : List.of());
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
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
