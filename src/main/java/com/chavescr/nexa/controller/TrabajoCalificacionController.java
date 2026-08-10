package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.FilaNotaTrabajo;
import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.ModuloAcademico;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.HistorialCambioService;
import com.chavescr.nexa.service.TrabajoExtraclaseService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/extraclase/evaluacion")
public class TrabajoCalificacionController {

    private final TrabajoExtraclaseService service;
    private final HistorialCambioService historialService;

    public TrabajoCalificacionController(TrabajoExtraclaseService service, HistorialCambioService historialService) {
        this.service = service;
        this.historialService = historialService;
    }

    @GetMapping("/modal")
    public String modal(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long trabajoId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarModal(model, institucionId, nivelId, materiaId, trabajoId);
        return "gestion-academica/extraclase/evaluacion-modal :: modal-content";
    }

    @PostMapping("/guardar-lote")
    public String guardarLote(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long trabajoId,
            @RequestParam(required = false) List<Long> estudianteId,
            @RequestParam(required = false) List<String> puntosObtenidos,
            @RequestParam(required = false) List<String> observacion,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);

        List<String> errores = new ArrayList<>();
        List<String> calificados = new ArrayList<>();
        if (estudianteId != null) {
            for (int i = 0; i < estudianteId.size(); i++) {
                String puntosStr = puntosObtenidos != null && i < puntosObtenidos.size() ? puntosObtenidos.get(i) : null;
                if (puntosStr == null || puntosStr.isBlank()) {
                    continue;
                }
                String obs = observacion != null && i < observacion.size() ? observacion.get(i) : null;
                try {
                    Integer puntos = Integer.valueOf(puntosStr.trim());
                    FilaNotaTrabajo fila = service.registrarCalificacion(institucionId, trabajoId, estudianteId.get(i),
                            puntos, obs);
                    calificados.add(fila.getEstudiante().getNombre() + ": " + fila.getCalificacion() + "%");
                } catch (NumberFormatException e) {
                    errores.add("unos puntos obtenidos inválidos");
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }
        }
        if (!calificados.isEmpty()) {
            String trabajoTitulo = service.obtenerTrabajo(institucionId, trabajoId).getTitulo();
            historialService.registrar(institucionId, nivelId, materiaId, ModuloAcademico.EXTRACLASE, trabajoId,
                    trabajoTitulo, AccionHistorial.CALIFICAR, usuario != null ? usuario.getId() : null,
                    usuario != null ? usuario.getNombre() : null, String.join(", ", calificados));
        }
        String trabajoCalificado = "\"trabajoExtraclaseCalificado\":{\"nivelId\":" + nivelId + ",\"materiaId\":" + materiaId + "}";
        if (!errores.isEmpty()) {
            String mensaje = errores.size() + " calificación(es) no se guardaron: "
                    + String.join("; ", errores.stream().distinct().toList());
            response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                    + "\"promedioDesactualizado\":\"\"," + trabajoCalificado + "}");
        } else {
            response.setHeader("HX-Trigger",
                    "{\"academicoGuardado\":{\"mensaje\":\"Calificaciones guardadas correctamente\"},"
                            + "\"promedioDesactualizado\":\"\"," + trabajoCalificado + "}");
        }
        cargarModal(model, institucionId, nivelId, materiaId, trabajoId);
        return "gestion-academica/extraclase/evaluacion-modal :: modal-content";
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void cargarModal(Model model, Long institucionId, Long nivelId, Long materiaId, Long trabajoId) {
        var trabajo = service.obtenerTrabajo(institucionId, trabajoId);
        model.addAttribute("trabajo", trabajo);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("periodoActivo", trabajo.getPeriodo());
        model.addAttribute("filas", service.listarNotas(institucionId, trabajoId));
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
