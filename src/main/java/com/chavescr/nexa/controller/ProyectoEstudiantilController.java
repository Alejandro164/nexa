package com.chavescr.nexa.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.ProyectoDefinicion;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.ProyectoEstudiantilService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/proyectos")
public class ProyectoEstudiantilController {

    private final ProyectoEstudiantilService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public ProyectoEstudiantilController(ProyectoEstudiantilService service,
            AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String proyectos(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Long proyectoId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, proyectoId, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    @PostMapping("/nuevo")
    public String nuevoProyecto(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model,
            HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        ProyectoDefinicion creado = service.crearProyecto(institucionId, nivelId, materiaId);
        response.setHeader("HX-Trigger", "promedioDesactualizado");
        cargarPanel(model, institucionId, nivelId, materiaId, creado.getId(), docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizarProyecto(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            @RequestParam(required = false) Integer porcentaje, @RequestParam(required = false) Integer puntosTotales,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.actualizarProyecto(institucionId, id, porcentaje, puntosTotales);
            response.setHeader("HX-Trigger", "promedioDesactualizado");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, id, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarProyecto(institucionId, id);
        response.setHeader("HX-Trigger", "promedioDesactualizado");
        cargarPanel(model, institucionId, nivelId, materiaId, null, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    @PostMapping("/guardar-lote")
    public String guardarLote(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long proyectoId,
            @RequestParam(required = false) List<Long> estudianteId,
            @RequestParam(required = false) List<String> puntosObtenidos,
            @RequestParam(required = false) List<String> observacion,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);

        List<String> errores = new ArrayList<>();
        if (estudianteId != null) {
            for (int i = 0; i < estudianteId.size(); i++) {
                String puntosStr = puntosObtenidos != null && i < puntosObtenidos.size() ? puntosObtenidos.get(i) : null;
                if (puntosStr == null || puntosStr.isBlank()) {
                    continue;
                }
                String obs = observacion != null && i < observacion.size() ? observacion.get(i) : null;
                try {
                    Integer puntos = Integer.valueOf(puntosStr.trim());
                    service.guardarNota(institucionId, proyectoId, estudianteId.get(i), puntos, obs);
                } catch (NumberFormatException e) {
                    errores.add("unos puntos obtenidos inválidos");
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }
        }
        if (!errores.isEmpty()) {
            model.addAttribute("error",
                    errores.size() + " calificación(es) no se guardaron: " + String.join("; ", errores.stream().distinct().toList()));
        }
        response.setHeader("HX-Trigger", "promedioDesactualizado");
        cargarPanel(model, institucionId, nivelId, materiaId, proyectoId, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    private void cargarPanel(Model model, Long institucionId, Long nivelId, Long materiaId, Long proyectoId,
            Long docenteId) {
        var niveles = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }
        var periodoActivo = service.obtenerPeriodoActivo(institucionId);
        var proyectos = nivelId != null && materiaId != null
                ? service.listarProyectos(institucionId, nivelId, materiaId, periodoActivo.getId())
                : List.<ProyectoDefinicion>of();
        Long proyectoSolicitado = proyectoId;
        if (proyectoSolicitado == null || proyectos.stream().noneMatch(p -> p.getId().equals(proyectoSolicitado))) {
            proyectoId = proyectos.isEmpty() ? null : proyectos.get(0).getId();
        }
        Long proyectoFinal = proyectoId;
        ProyectoDefinicion proyectoSeleccionado = proyectoFinal != null
                ? proyectos.stream().filter(p -> p.getId().equals(proyectoFinal)).findFirst().orElse(null)
                : null;

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("proyectoId", proyectoId);
        model.addAttribute("proyectoSeleccionado", proyectoSeleccionado);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("filas", proyectoSeleccionado != null
                ? service.listarNotas(institucionId, proyectoSeleccionado.getId())
                : List.of());
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }

    private void exigirDocenteODirectorOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden evaluar");
        }
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
