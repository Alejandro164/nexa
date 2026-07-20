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

import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.service.IndicadorCotidianoService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/cotidiano/indicadores")
public class IndicadorCotidianoController {

    private final IndicadorCotidianoService service;

    public IndicadorCotidianoController(IndicadorCotidianoService service) {
        this.service = service;
    }

    @GetMapping
    public String indicadores(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, periodoId, materiaId);
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    @GetMapping("/form")
    public String nuevoIndicador(@RequestParam Long periodoId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("indicador", new IndicadorCotidiano());
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/cotidiano/indicador-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarIndicador(@PathVariable Long id, @RequestParam Long periodoId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("indicador", service.obtenerIndicador(institucionId, id));
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/cotidiano/indicador-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long periodoId, @RequestParam Long materiaId,
            @ModelAttribute IndicadorCotidiano indicador, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.guardarIndicador(institucionId, periodoId, materiaId, indicador);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, institucionId, periodoId, materiaId);
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long periodoId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarIndicador(institucionId, id);
        cargarPanel(model, institucionId, periodoId, materiaId);
        return "gestion-academica/cotidiano/indicadores :: content";
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
        List<IndicadorCotidiano> indicadores = periodoId != null && materiaId != null
                ? service.listarIndicadores(institucionId, periodoId, materiaId)
                : List.of();
        int total = indicadores.stream().mapToInt(IndicadorCotidiano::getPorcentaje).sum();
        model.addAttribute("periodos", periodos);
        model.addAttribute("materias", materias);
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("indicadores", indicadores);
        model.addAttribute("totalAsignado", total);
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }
}
