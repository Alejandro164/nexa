package com.chavescr.nexa.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.EvaluacionCotidianaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/cotidiano/evaluacion")
public class EvaluacionCotidianaController {

    private final EvaluacionCotidianaService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public EvaluacionCotidianaController(EvaluacionCotidianaService service,
            AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String evaluaciones(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Long indicadorId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, indicadorId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/evaluacion :: content";
    }

    @PostMapping("/guardar-lote")
    public String guardarLote(@RequestParam Long nivelId, @RequestParam Long materiaId, @RequestParam Long indicadorId,
            @RequestParam(required = false) List<Long> estudianteId,
            @RequestParam(required = false) List<String> calificacion,
            @RequestParam(required = false) List<String> observacion,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        Long periodoId = service.obtenerPeriodoActivo(institucionId).getId();

        List<String> errores = new ArrayList<>();
        if (estudianteId != null) {
            for (int i = 0; i < estudianteId.size(); i++) {
                String calStr = calificacion != null && i < calificacion.size() ? calificacion.get(i) : null;
                if (calStr == null || calStr.isBlank()) {
                    continue;
                }
                String obs = observacion != null && i < observacion.size() ? observacion.get(i) : null;
                try {
                    Integer cal = Integer.valueOf(calStr.trim());
                    service.registrarCalificacion(institucionId, estudianteId.get(i), indicadorId, periodoId, cal, obs);
                } catch (NumberFormatException e) {
                    errores.add("una calificación inválida");
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
        cargarPanel(model, institucionId, nivelId, materiaId, indicadorId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/evaluacion :: content";
    }

    private void cargarPanel(Model model, Long institucionId, Long nivelId, Long materiaId, Long indicadorId,
            Long docenteId) {
        var niveles = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }

        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }

        var indicadores = nivelId != null && materiaId != null
                ? service.listarIndicadoresDisponibles(institucionId, nivelId, materiaId)
                : List.<IndicadorCotidiano>of();
        Long indicadorSolicitado = indicadorId;
        if (indicadorSolicitado == null || indicadores.stream().noneMatch(i -> i.getId().equals(indicadorSolicitado))) {
            indicadorId = indicadores.isEmpty() ? null : indicadores.get(0).getId();
        }

        var periodoActivo = service.obtenerPeriodoActivo(institucionId);

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("indicadores", indicadores);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("indicadorId", indicadorId);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("filas", nivelId != null && indicadorId != null
                ? service.listarFilas(institucionId, nivelId, indicadorId, periodoActivo.getId())
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
