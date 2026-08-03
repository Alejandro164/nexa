package com.chavescr.nexa.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.AsistenciaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/asistencia")
public class AsistenciaController {

    private final AsistenciaService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public AsistenciaController(AsistenciaService service, AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String asistencia(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, fecha, docenteIdSiAplica(request, session));
        return "gestion-academica/asistencia/asistencia :: content";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Long estudianteId,
            @RequestParam Long nivelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String observaciones,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        Long registradoPorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            service.registrarEstado(institucionId, estudianteId, nivelId, fecha, estado, observaciones,
                    registradoPorId);
        } catch (DataIntegrityViolationException e) {
            // otra petición concurrente insertó el registro primero; reintentar una vez ya que existe
            try {
                service.registrarEstado(institucionId, estudianteId, nivelId, fecha, estado, observaciones,
                        registradoPorId);
            } catch (IllegalArgumentException e2) {
                model.addAttribute("error", e2.getMessage());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, fecha, docenteIdSiAplica(request, session));
        return "gestion-academica/asistencia/asistencia :: content";
    }

    private void cargarPanel(Model model, Long institucionId, Long nivelId, LocalDate fecha, Long docenteId) {
        var secciones = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        if (nivelId == null && !secciones.isEmpty()) {
            nivelId = secciones.get(0).getId();
        }
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        model.addAttribute("secciones", secciones);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("fecha", fecha);
        model.addAttribute("filas", nivelId != null ? service.listarFilas(institucionId, nivelId, fecha) : List.of());
    }

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = institucionId(session);
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }

    private void exigirDocenteODirectorOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden registrar asistencia");
        }
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
