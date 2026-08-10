package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.service.ProyectoEstudiantilService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/proyectos/evaluacion")
public class ProyectoCalificacionController {

    private final ProyectoEstudiantilService service;

    public ProyectoCalificacionController(ProyectoEstudiantilService service) {
        this.service = service;
    }

    @GetMapping("/modal")
    public String modal(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long proyectoId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarModal(model, institucionId, nivelId, materiaId, proyectoId);
        return "gestion-academica/proyectos/evaluacion-modal :: modal-content";
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
                    service.registrarCalificacion(institucionId, proyectoId, estudianteId.get(i), puntos, obs);
                } catch (NumberFormatException e) {
                    errores.add("unos puntos obtenidos inválidos");
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }
        }
        String proyectoCalificado = "\"proyectoCalificado\":{\"nivelId\":" + nivelId + ",\"materiaId\":" + materiaId + "}";
        if (!errores.isEmpty()) {
            String mensaje = errores.size() + " calificación(es) no se guardaron: "
                    + String.join("; ", errores.stream().distinct().toList());
            response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                    + "\"promedioDesactualizado\":\"\"," + proyectoCalificado + "}");
        } else {
            response.setHeader("HX-Trigger",
                    "{\"academicoGuardado\":{\"mensaje\":\"Calificaciones guardadas correctamente\"},"
                            + "\"promedioDesactualizado\":\"\"," + proyectoCalificado + "}");
        }
        cargarModal(model, institucionId, nivelId, materiaId, proyectoId);
        return "gestion-academica/proyectos/evaluacion-modal :: modal-content";
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void cargarModal(Model model, Long institucionId, Long nivelId, Long materiaId, Long proyectoId) {
        var proyecto = service.obtenerProyecto(institucionId, proyectoId);
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("periodoActivo", proyecto.getPeriodo());
        model.addAttribute("filas", service.listarNotas(institucionId, proyectoId));
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }

    private void exigirDocenteODirectorOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden evaluar");
        }
    }
}
