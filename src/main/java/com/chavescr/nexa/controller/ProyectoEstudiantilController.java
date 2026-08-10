package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.ModuloAcademico;
import com.chavescr.nexa.entity.ProyectoDefinicion;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.HistorialCambioService;
import com.chavescr.nexa.service.ProyectoEstudiantilService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/proyectos")
public class ProyectoEstudiantilController {

    private final ProyectoEstudiantilService service;
    private final AlcanceDocenteService alcanceDocenteService;
    private final HistorialCambioService historialService;

    public ProyectoEstudiantilController(ProyectoEstudiantilService service,
            AlcanceDocenteService alcanceDocenteService, HistorialCambioService historialService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
        this.historialService = historialService;
    }

    @GetMapping
    public String proyectos(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    @GetMapping("/form")
    public String nuevoProyecto(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("proyecto", new ProyectoDefinicion());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/proyectos/proyecto-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarProyecto(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("proyecto", service.obtenerProyecto(institucionId, id));
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/proyectos/proyecto-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @ModelAttribute ProyectoDefinicion proyecto, Model model, HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        Long institucionId = requerirInstitucion(session);
        boolean esNuevo = proyecto.getId() == null;
        try {
            ProyectoDefinicion guardado = service.guardarProyecto(institucionId, nivelId, materiaId, proyecto);
            historialService.registrar(institucionId, nivelId, materiaId, ModuloAcademico.PROYECTO, guardado.getId(),
                    guardado.getTitulo(), esNuevo ? AccionHistorial.CREAR : AccionHistorial.EDITAR,
                    usuario != null ? usuario.getId() : null, usuario != null ? usuario.getNombre() : null);
            notificarGuardado(response, esNuevo ? "Proyecto creado correctamente" : "Proyecto actualizado correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        Long institucionId = requerirInstitucion(session);
        try {
            ProyectoDefinicion proyecto = service.obtenerProyecto(institucionId, id);
            service.eliminarProyecto(institucionId, id);
            historialService.registrar(institucionId, nivelId, materiaId, ModuloAcademico.PROYECTO, proyecto.getId(),
                    proyecto.getTitulo(), AccionHistorial.ELIMINAR,
                    usuario != null ? usuario.getId() : null, usuario != null ? usuario.getNombre() : null);
            notificarGuardado(response, "Proyecto eliminado correctamente");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/proyectos/proyectos :: content";
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
        List<ProyectoDefinicion> proyectos = nivelId != null && materiaId != null && periodoActivo != null
                ? service.listarProyectos(institucionId, nivelId, materiaId, periodoActivo.getId())
                : List.of();
        int total = proyectos.stream().mapToInt(ProyectoDefinicion::getPorcentaje).sum();

        int totalEstudiantesSeccion = nivelId != null ? service.contarEstudiantesActivos(nivelId) : 0;
        var evaluadosPorProyecto = service.contarEvaluadosPorProyecto(proyectos.stream().map(ProyectoDefinicion::getId).toList());
        var promedioPorProyecto = service.calcularPromedioPorProyecto(proyectos.stream().map(ProyectoDefinicion::getId).toList());

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("totalAsignado", total);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("totalEstudiantesSeccion", totalEstudiantesSeccion);
        model.addAttribute("evaluadosPorProyecto", evaluadosPorProyecto);
        model.addAttribute("promedioPorProyecto", promedioPorProyecto);
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
