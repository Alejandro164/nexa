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

import com.chavescr.nexa.entity.EvaluacionCotidiana;
import com.chavescr.nexa.service.EvaluacionCotidianaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/cotidiano/evaluacion")
public class EvaluacionCotidianaController {

    private final EvaluacionCotidianaService service;

    public EvaluacionCotidianaController(EvaluacionCotidianaService service) {
        this.service = service;
    }

    @GetMapping
    public String evaluaciones(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId);
        return "gestion-academica/cotidiano/evaluacion :: content";
    }

    @GetMapping("/form")
    public String nuevaEvaluacion(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model,
            HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("evaluacion", new EvaluacionCotidiana());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("estudiantes", service.listarEstudiantesDeSeccion(nivelId));
        model.addAttribute("indicadoresDisponibles", service.listarIndicadoresDisponibles(institucionId, materiaId));
        return "gestion-academica/cotidiano/evaluacion-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarEvaluacion(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("evaluacion", service.obtenerEvaluacion(institucionId, id));
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("estudiantes", service.listarEstudiantesDeSeccion(nivelId));
        model.addAttribute("indicadoresDisponibles", service.listarIndicadoresDisponibles(institucionId, materiaId));
        return "gestion-academica/cotidiano/evaluacion-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @RequestParam Long indicadorId, @RequestParam Long estudianteId,
            @ModelAttribute EvaluacionCotidiana evaluacion, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.guardarEvaluacion(institucionId, indicadorId, estudianteId, evaluacion);
        cargarPanel(model, institucionId, nivelId, materiaId);
        return "gestion-academica/cotidiano/evaluacion :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarEvaluacion(institucionId, id);
        cargarPanel(model, institucionId, nivelId, materiaId);
        return "gestion-academica/cotidiano/evaluacion :: content";
    }

    private void cargarPanel(Model model, Long institucionId, Long nivelId, Long materiaId) {
        var niveles = service.listarNivelesActivos(institucionId);
        var materias = service.listarMateriasActivas(institucionId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }
        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("evaluaciones", nivelId != null && materiaId != null
                ? service.listarEvaluaciones(institucionId, nivelId, materiaId)
                : List.of());
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }
}
