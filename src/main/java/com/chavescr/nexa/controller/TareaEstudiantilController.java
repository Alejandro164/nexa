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

import com.chavescr.nexa.entity.TareaEstudiantil;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.TareaEstudiantilService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/tareas")
public class TareaEstudiantilController {

    private final TareaEstudiantilService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public TareaEstudiantilController(TareaEstudiantilService service, AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String tareas(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/tareas/tareas :: content";
    }

    @GetMapping("/form")
    public String nuevaTarea(@RequestParam Long periodoId, @RequestParam Long materiaId, Model model,
            HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tarea", new TareaEstudiantil());
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("estudiantes", service.listarEstudiantesActivos(institucionId));
        return "gestion-academica/tareas/form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarTarea(@PathVariable Long id, @RequestParam Long periodoId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tarea", service.obtenerTarea(institucionId, id));
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("estudiantes", service.listarEstudiantesActivos(institucionId));
        return "gestion-academica/tareas/form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long periodoId, @RequestParam Long materiaId,
            @RequestParam Long estudianteId, @ModelAttribute TareaEstudiantil tarea, Model model,
            HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        service.guardarTarea(institucionId, periodoId, materiaId, estudianteId, tarea);
        cargarPanel(model, institucionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/tareas/tareas :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long periodoId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarTarea(institucionId, id);
        cargarPanel(model, institucionId, periodoId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/tareas/tareas :: content";
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
        model.addAttribute("tareas", periodoId != null && materiaId != null
                ? service.listarTareas(institucionId, periodoId, materiaId)
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
