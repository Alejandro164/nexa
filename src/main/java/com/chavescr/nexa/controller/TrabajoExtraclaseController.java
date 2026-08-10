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

import com.chavescr.nexa.entity.TrabajoDefinicion;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.TrabajoExtraclaseService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/extraclase")
public class TrabajoExtraclaseController {

    private final TrabajoExtraclaseService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public TrabajoExtraclaseController(TrabajoExtraclaseService service,
            AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String trabajos(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/extraclase/extraclase :: content";
    }

    @GetMapping("/form")
    public String nuevoTrabajo(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("trabajo", new TrabajoDefinicion());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/extraclase/trabajo-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarTrabajo(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("trabajo", service.obtenerTrabajo(institucionId, id));
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/extraclase/trabajo-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @ModelAttribute TrabajoDefinicion trabajo, Model model, HttpSession session,
            HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        boolean esNuevo = trabajo.getId() == null;
        try {
            service.guardarTrabajo(institucionId, nivelId, materiaId, trabajo);
            notificarGuardado(response, esNuevo ? "Trabajo creado correctamente" : "Trabajo actualizado correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/extraclase/extraclase :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.eliminarTrabajo(institucionId, id);
            notificarGuardado(response, "Trabajo eliminado correctamente");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/extraclase/extraclase :: content";
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

        var periodoActivo = service.obtenerPeriodoActivoOpcional(institucionId);
        List<TrabajoDefinicion> trabajos = nivelId != null && materiaId != null && periodoActivo != null
                ? service.listarTrabajos(institucionId, nivelId, materiaId, periodoActivo.getId())
                : List.of();
        int total = trabajos.stream().mapToInt(TrabajoDefinicion::getPorcentaje).sum();

        int totalEstudiantesSeccion = nivelId != null ? service.contarEstudiantesActivos(nivelId) : 0;
        var evaluadosPorTrabajo = service.contarEvaluadosPorTrabajo(trabajos.stream().map(TrabajoDefinicion::getId).toList());
        var promedioPorTrabajo = service.calcularPromedioPorTrabajo(trabajos.stream().map(TrabajoDefinicion::getId).toList());

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("trabajos", trabajos);
        model.addAttribute("totalAsignado", total);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("totalEstudiantesSeccion", totalEstudiantesSeccion);
        model.addAttribute("evaluadosPorTrabajo", evaluadosPorTrabajo);
        model.addAttribute("promedioPorTrabajo", promedioPorTrabajo);
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
