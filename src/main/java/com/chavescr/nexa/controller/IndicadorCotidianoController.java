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
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.IndicadorCotidianoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/cotidiano/indicadores")
public class IndicadorCotidianoController {

    private final IndicadorCotidianoService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public IndicadorCotidianoController(IndicadorCotidianoService service,
            AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String indicadores(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    @GetMapping("/form")
    public String nuevoIndicador(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("indicador", new IndicadorCotidiano());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/cotidiano/indicador-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarIndicador(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("indicador", service.obtenerIndicador(institucionId, id));
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/cotidiano/indicador-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @ModelAttribute IndicadorCotidiano indicador, Model model, HttpSession session,
            HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.guardarIndicador(institucionId, nivelId, materiaId, indicador);
            notificarIndicadoresActualizados(response);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarIndicador(institucionId, id);
        notificarIndicadoresActualizados(response);
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    /** Avisa a la pestaña de Evaluación (ya cargada aparte) que los indicadores cambiaron, para que se refresque. */
    private void notificarIndicadoresActualizados(HttpServletResponse response) {
        response.setHeader("HX-Trigger", "indicadorCotidianoActualizado");
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
        List<IndicadorCotidiano> indicadores = nivelId != null && materiaId != null
                ? service.listarIndicadores(institucionId, nivelId, materiaId)
                : List.of();
        int total = indicadores.stream().mapToInt(IndicadorCotidiano::getPorcentaje).sum();
        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
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

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
