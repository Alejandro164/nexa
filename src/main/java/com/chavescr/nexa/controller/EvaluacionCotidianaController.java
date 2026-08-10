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

import com.chavescr.nexa.service.EvaluacionCotidianaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/cotidiano/evaluacion")
public class EvaluacionCotidianaController {

    private final EvaluacionCotidianaService service;

    public EvaluacionCotidianaController(EvaluacionCotidianaService service) {
        this.service = service;
    }

    @GetMapping("/modal")
    public String modal(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long indicadorId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarModal(model, institucionId, nivelId, materiaId, indicadorId);
        return "gestion-academica/cotidiano/evaluacion-modal :: modal-content";
    }

    @PostMapping("/guardar-lote")
    public String guardarLote(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long indicadorId,
            @RequestParam(required = false) List<Long> estudianteId,
            @RequestParam(required = false) List<String> calificacion,
            @RequestParam(required = false) List<String> puntosObtenidos,
            @RequestParam(required = false) List<String> observacion,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        Long periodoId = service.obtenerPeriodoActivo(institucionId).getId();

        List<String> errores = new ArrayList<>();
        if (estudianteId != null) {
            for (int i = 0; i < estudianteId.size(); i++) {
                String calStr = calificacion != null && i < calificacion.size() ? calificacion.get(i) : null;
                String puntosStr = puntosObtenidos != null && i < puntosObtenidos.size() ? puntosObtenidos.get(i) : null;
                if ((calStr == null || calStr.isBlank()) && (puntosStr == null || puntosStr.isBlank())) {
                    continue;
                }
                String obs = observacion != null && i < observacion.size() ? observacion.get(i) : null;
                try {
                    Integer cal = calStr != null && !calStr.isBlank() ? Integer.valueOf(calStr.trim()) : null;
                    Integer puntos = puntosStr != null && !puntosStr.isBlank() ? Integer.valueOf(puntosStr.trim()) : null;
                    service.registrarCalificacion(institucionId, estudianteId.get(i), indicadorId, periodoId, cal, puntos, obs);
                } catch (NumberFormatException e) {
                    errores.add("un valor inválido");
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }
        }
        String cotidianoCalificado = "\"cotidianoCalificado\":{\"nivelId\":" + nivelId + ",\"materiaId\":" + materiaId + "}";
        if (!errores.isEmpty()) {
            String mensaje = errores.size() + " calificación(es) no se guardaron: "
                    + String.join("; ", errores.stream().distinct().toList());
            response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                    + "\"promedioDesactualizado\":\"\"," + cotidianoCalificado + "}");
        } else {
            response.setHeader("HX-Trigger",
                    "{\"academicoGuardado\":{\"mensaje\":\"Calificaciones guardadas correctamente\"},"
                            + "\"promedioDesactualizado\":\"\"," + cotidianoCalificado + "}");
        }
        cargarModal(model, institucionId, nivelId, materiaId, indicadorId);
        return "gestion-academica/cotidiano/evaluacion-modal :: modal-content";
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void cargarModal(Model model, Long institucionId, Long nivelId, Long materiaId, Long indicadorId) {
        var indicador = service.obtenerIndicador(institucionId, indicadorId);
        var periodoActivo = service.obtenerPeriodoActivo(institucionId);
        model.addAttribute("indicador", indicador);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("filas", service.listarFilas(institucionId, nivelId, indicadorId, periodoActivo.getId()));
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
