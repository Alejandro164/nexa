package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

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

import com.chavescr.nexa.entity.TareaDefinicion;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.TareaCalificacionService;
import com.chavescr.nexa.service.TareaDefinicionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/tareas/definiciones")
public class TareaDefinicionController {

    private final TareaDefinicionService service;
    private final AlcanceDocenteService alcanceDocenteService;
    private final TareaCalificacionService evaluacionService;

    public TareaDefinicionController(TareaDefinicionService service, AlcanceDocenteService alcanceDocenteService,
            TareaCalificacionService evaluacionService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
        this.evaluacionService = evaluacionService;
    }

    @GetMapping
    public String tareas(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/tareas/definiciones :: content";
    }

    @GetMapping("/form")
    public String nuevaTarea(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("tarea", new TareaDefinicion());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/tareas/definicion-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarTarea(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tarea", service.obtenerTarea(institucionId, id));
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/tareas/definicion-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @ModelAttribute TareaDefinicion tarea, Model model, HttpSession session,
            HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        boolean esNueva = tarea.getId() == null;
        try {
            service.guardarTarea(institucionId, nivelId, materiaId, tarea);
            notificarGuardado(response, esNueva ? "Tarea creada correctamente" : "Tarea actualizada correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/tareas/definiciones :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.eliminarTarea(institucionId, id);
            notificarGuardado(response, "Tarea eliminada correctamente");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/tareas/definiciones :: content";
    }

    private void notificarGuardado(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoGuardado\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                + "\"promedioDesactualizado\":\"\"}");
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void cargarPanel(Model model, Long institucionId, Long nivelId, Long materiaId, Long docenteId) {
        var niveles = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }
        List<TareaDefinicion> tareas = nivelId != null && materiaId != null
                ? service.listarTareas(institucionId, nivelId, materiaId)
                : List.of();
        int total = tareas.stream().mapToInt(TareaDefinicion::getPorcentaje).sum();

        var periodoActivo = evaluacionService.obtenerPeriodoActivoOpcional(institucionId);
        int totalEstudiantesSeccion = nivelId != null ? evaluacionService.contarEstudiantesActivos(nivelId) : 0;
        var evaluadosPorTarea = evaluacionService.contarEvaluadosPorTarea(institucionId,
                tareas.stream().map(TareaDefinicion::getId).toList(),
                periodoActivo != null ? periodoActivo.getId() : null);
        var promedioPorTarea = evaluacionService.calcularPromedioPorTarea(institucionId,
                tareas.stream().map(TareaDefinicion::getId).toList(),
                periodoActivo != null ? periodoActivo.getId() : null);

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("tareas", tareas);
        model.addAttribute("totalAsignado", total);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("totalEstudiantesSeccion", totalEstudiantesSeccion);
        model.addAttribute("evaluadosPorTarea", evaluadosPorTarea);
        model.addAttribute("promedioPorTarea", promedioPorTarea);
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
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
